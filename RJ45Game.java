//
// Created by juh on 14/11/26 on IntelliJ
// v.11.1
// (tava sem coragem de postar o andamento das versões anteriores)

import javax.swing.*; // importa todas as classes do Swing (JFrame, JPanel, JButton...), a biblioteca de janelas do Java
import javax.swing.table.DefaultTableCellRenderer; // renderizador padrão das células de uma JTable (usado para colorir o ranking)
import javax.swing.table.DefaultTableModel; // modelo de dados padrão de uma JTable (linhas e colunas do ranking)
import java.awt.*; // AWT: cores, fontes, Graphics, layouts e formas básicas
import java.awt.event.*; // eventos de mouse, teclado e ação (ActionEvent, MouseAdapter...)
import java.awt.geom.*; // formas geométricas 2D (Path2D, Line2D, RoundRectangle2D...)
import java.io.IOException; // exceção de entrada/saída, lançada ao ler ou gravar arquivos
import java.nio.charset.StandardCharsets; // constante do conjunto de caracteres UTF-8
import java.nio.file.*; // API de arquivos (Path, Paths, Files e as opções de abertura)
import java.security.MessageDigest; // calcula resumos criptográficos (hash) e os compara
import java.time.LocalDate; // data sem horário, usada para registrar o dia da partida
import java.util.*; // coleções e utilitários (ArrayList, HashMap, Arrays, Collections, Comparator)
import java.util.List; // desfaz a ambiguidade: "List" passa a ser java.util.List, e não java.awt.List

/**
 * Jogo: monte o cabo de rede arrastando os 8 fios para os pinos corretos.
 * Fase 1: plugue RJ45 T568A  ->  Fase 2: plugue RJ45 T568B  ->  Fase 3: keystone Cat5e (punch-down)
 * Cada teste errado da direito a 1 dica (acumulam). So o usuario admin ve a opcao "Mostrar dicas".
 * O admin entra com um login protegido por senha, aparece sempre como "Juju_Admin" e pode excluir partidas do ranking.
 * O jogo abre em um menu inicial (Novo jogo / Ranking / Sair); o nome do jogador e pedido ao clicar em Novo jogo.
 * Com os 8 fios encaixados aparece na bandeja o plugue para conectar no testador ("Conectar e testar o cabo").
 * Errou: refazer mantem os fios certos no lugar. Na keystone da para desistir (salva o que fez no ranking).
 * No fim (ou na desistencia) abre a tela de resultado: estrelas por fase (1 = vermelha, 2 = amarela, 3 = verde),
 * tempo, testes, quantas dicas foram usadas e a pontuacao.
 * Compilar: javac RJ45Game.java   |   Executar: java RJ45Game
 *
 * Atalhos no jogo: T ou Enter = testar | R = embaralhar fios | D = usar dica | N = nova partida | K = ranking | M = menu
 * Atalhos no menu: setas = escolher | Enter = confirmar | Esc = voltar (nas telas de nome e ranking)
 */
public class RJ45Game extends JFrame { // herança = a classe principal É uma janela, "extends JFrame" herda tudo que uma janela do Swing sabe fazer

    // ids dos fios, na ordem do padrao T568B (pino 1..8)
    static final String[] NOMES = {"branco-laranja", "laranja", "branco-verde", "azul", // constante da classe: static = pertence à classe, final = não muda. Vetor com os nomes dos 8 fios
                                   "branco-azul", "verde", "branco-marrom", "marrom"}; // (continuação do vetor) os quatro últimos nomes
    static final Color[] CORES = { // vetor com a cor de cada fio, na mesma ordem de NOMES
        new Color(255, 140, 0), new Color(255, 140, 0), // fios 0 e 1: laranja (new Color(vermelho, verde, azul))
        new Color(0, 190, 0),   new Color(0, 90, 230), // fios 2 e 3: verde e azul
        new Color(0, 90, 230),  new Color(0, 190, 0), // fios 4 e 5: azul e verde
        new Color(120, 70, 20), new Color(120, 70, 20)}; // fios 6 e 7: marrom; "};" fecha o vetor
    static final boolean[] LISTRADO = {true, false, true, false, true, false, true, false}; // vetor de boolean: true = fio listrado (branco com a cor), false = fio de cor inteira

    // padrao[pino] = id do fio que deve estar naquele pino
    static final int[] T568B = {0, 1, 2, 3, 4, 5, 6, 7}; // T568B: o pino i recebe o fio i, porque a ordem de NOMES já é a do T568B
    static final int[] T568A = {2, 5, 0, 3, 4, 1, 6, 7}; // T568A: em relação ao T568B, troca de lugar os pares laranja e verde

    // Posicao dos pinos no keystone (linhas de cima para baixo), conforme a etiqueta 4-5-2-1 / 6-3-8-7
    static final int[] KS_ESQ = {3, 4, 1, 0};   // pinos 4,5,2,1; coluna esquerda da keystone: qual pino (índice 0..7) fica em cada linha
    static final int[] KS_DIR = {5, 2, 7, 6};   // pinos 6,3,8,7; coluna direita da keystone

    // As 3 fases da partida, em ordem
    static final String[] FASES = {"RJ45 T568A", "RJ45 T568B", "Keystone Cat5e"}; // nomes das fases, mostrados na tela
    static final int[][] PADRAO_FASE = {T568A, T568B, T568B}; // matriz = vetor de vetores: o padrão de cores exigido em cada fase
    static final int FASE_KEYSTONE = 2; // índice da fase da keystone (a terceira; a contagem começa em 0)
    static final int N_FASES = FASES.length; // quantidade de fases, calculada pelo tamanho do vetor

    // Administrador: ve a opcao "Mostrar dicas" e pode excluir partidas do ranking.
    // O login e a senha dele nao ficam escritos aqui, so os hashes; na tela ele aparece como NOME_ADMIN.
    static final String NOME_ADMIN = "Juju_Admin"; // nome público do administrador
    static final String SAL = "rj45game:"; // "sal": texto misturado ao login/senha antes do hash, para dificultar ataques por tabelas prontas
    static final String HASH_LOGIN_ADM = "9498412e1c684ce702b75c6d8a5f85fd2dc0ecf44e74400dac78c056b8183376";   // SHA-256(SAL + login); hash do login do adm (o login em si não aparece no código)
    static final String HASH_SENHA_ADM = "0de7ecc0f2ced75aaa667199ebe4f48e0625687df9d5dd24e5d4be1490db3d7c";   // PBKDF2(senha, SAL); hash da senha do adm
    static final int VOLTAS_SENHA = 120000; // número de repetições do PBKDF2: deixa lenta a tentativa de adivinhar a senha

    static String emHex(byte[] b) { // método estático = converte um vetor de bytes em texto hexadecimal
        StringBuilder sb = new StringBuilder(); // StringBuilder monta um texto aos poucos, sem criar uma String nova a cada passo
        for (byte x : b) sb.append(String.format("%02x", x)); // for-each = para cada byte, acrescenta dois dígitos hexadecimais
        return sb.toString(); // retorna o texto pronto
    }

    static boolean mesmoHash(String a, String b) {   // comparacao em tempo constante; método que compara dois hashes e retorna true se forem iguais
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); // MessageDigest.isEqual demora sempre o mesmo tempo, sem revelar em que ponto os textos diferem
    }

    /** O nome digitado e o login do administrador? */
    static boolean loginAdm(String nome) { // método que retorna true se o nome digitado é o login do administrador
        try { // try/catch = o cálculo do hash pode lançar uma exceção
            byte[] h = MessageDigest.getInstance("SHA-256").digest((SAL + nome).getBytes(StandardCharsets.UTF_8)); // calcula o SHA-256 de (sal + nome)
            return mesmoHash(emHex(h), HASH_LOGIN_ADM); // compara com o hash guardado na constante
        } catch (java.security.GeneralSecurityException e) { // exceção = se o algoritmo não existir nesta JVM...
            return false; // ...trata como "não é o adm"
        }
    }

    static boolean senhaAdm(char[] senha) { // método que confere a senha do adm; recebe char[] (e não String) p/ poder apagá-la da memória depois
        try { // bloco protegido contra exceções
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(senha, // especificação da chave: a senha,
                    SAL.getBytes(StandardCharsets.UTF_8), VOLTAS_SENHA, 256); // o sal em bytes, o número de voltas e o tamanho do resultado (256 bits)
            byte[] h = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); // deriva a chave com PBKDF2-HMAC-SHA256 e pega os bytes do resultado
            spec.clearPassword(); // apaga a cópia da senha guardada na especificação
            return mesmoHash(emHex(h), HASH_SENHA_ADM); // compara o resultado com o hash guardado
        } catch (java.security.GeneralSecurityException e) { // erro de criptografia...
            return false; // ...conta como senha errada
        } finally { // finally = este bloco executa sempre, com erro ou sem
            Arrays.fill(senha, '\0'); // sobrescreve a senha digitada com caracteres nulos
        }
    }

    static final int LARGURA = 1000, ALTURA = 650;   // tamanho logico da area de jogo; duas constantes inteiras declaradas na mesma linha
    static final Path ARQ_RANKING = Paths.get(System.getProperty("user.home"), ".rj45game_partidas.csv"); // caminho do arquivo do ranking: pasta pessoal do usuário + nome do arquivo

    static final Color FUNDO = new Color(30, 34, 46), CLARO = new Color(222, 228, 240), BORDA = new Color(96, 104, 124); // cores do tema: fundo, texto claro e borda
    static final Color AZUL_CABO = new Color(30, 110, 220), DESTAQUE = new Color(80, 200, 255); // azul do cabo e azul de destaque
    static final Color VERDE = new Color(0, 230, 100), AMARELO = new Color(255, 210, 60), VERMELHO = new Color(240, 70, 70); // cores de estado: verde (certo), amarelo (atenção) e vermelho (erro)
    static final Color CINZA = new Color(150, 160, 182); // cinza para textos apagados

    static { temaEscuro(); }   // antes de criar qualquer componente; bloco estático = roda uma única vez, quando a classe é carregada pela JVM

    /** Deixa os componentes padrao do Swing (dialogos, botoes, campos) com as cores do menu inicial. */
    static void temaEscuro() { // método que aplica o tema escuro aos componentes padrão do Swing
        Color botao = new Color(56, 62, 80); // cor de fundo dos botões
        Object[] tema = { // vetor de Object = pares "chave do Swing, valor". Object é a superclasse de tudo, então aceita String, Color, Font...
            "Panel.background", FUNDO, "OptionPane.background", FUNDO, "OptionPane.messageForeground", CLARO, // fundo dos painéis e dos diálogos, cor do texto das mensagens
            "Label.foreground", CLARO, "CheckBox.background", FUNDO, "CheckBox.foreground", CLARO, // cor do texto dos rótulos e cores das caixas de seleção
            "Button.background", botao, "Button.foreground", Color.WHITE, "Button.select", new Color(38, 43, 56), // cores dos botões: fundo, texto e fundo quando pressionado
            "Button.focus", DESTAQUE, "Button.disabledText", new Color(120, 128, 146), // cor do foco e do texto de botão desabilitado
            "Button.gradient", Arrays.asList(0.3f, 0f, botao, botao, botao), // gradiente do botão (todas as cores iguais = cor lisa)
            "Button.font", new Font(Font.DIALOG, Font.BOLD, 13), // fonte dos botões
            "Button.border", new javax.swing.plaf.BorderUIResource(BorderFactory.createCompoundBorder( // borda dos botões: uma linha arredondada...
                    BorderFactory.createLineBorder(BORDA, 1, true), BorderFactory.createEmptyBorder(7, 16, 7, 16))), // ...combinada com uma margem interna
            "TextField.background", new Color(18, 21, 30), "TextField.foreground", Color.WHITE, // cores do campo de texto: fundo e letra
            "TextField.caretForeground", DESTAQUE, "TextField.selectionBackground", AZUL_CABO, // cor do cursor e do fundo da seleção
            "TextField.selectionForeground", Color.WHITE, // cor do texto selecionado
            "ScrollBar.background", new Color(24, 28, 38), "ScrollBar.track", new Color(24, 28, 38), // barra de rolagem: fundo e trilho
            "ScrollBar.thumb", botao, "ScrollBar.thumbShadow", BORDA, "ScrollBar.thumbHighlight", BORDA, // barra de rolagem: a parte que se arrasta e seus contornos
            "ScrollBar.shadow", new Color(24, 28, 38), "ScrollBar.highlight", new Color(24, 28, 38), // barra de rolagem: sombras
            "ScrollBar.darkShadow", BORDA, "ScrollBar.gradient", Arrays.asList(0.3f, 0f, botao, botao, botao), // barra de rolagem: contorno escuro e gradiente
            "ScrollPane.background", FUNDO, // fundo da área com rolagem
            "TextField.border", new javax.swing.plaf.BorderUIResource(BorderFactory.createCompoundBorder( // borda do campo de texto: linha azul arredondada...
                    BorderFactory.createLineBorder(DESTAQUE, 1, true), BorderFactory.createEmptyBorder(5, 8, 5, 8))), // ...combinada com uma margem interna
        }; // fim do vetor
        for (int i = 0; i < tema.length; i += 2) UIManager.put(tema[i], tema[i + 1]); // percorre o vetor de 2 em 2, registrando cada chave com seu valor no UIManager (configuração global do Swing)
    }

    final JCheckBox dicasBox = new JCheckBox("Mostrar dicas (adm)", true); // atributo final = caixa "Mostrar dicas", que só o adm enxerga; começa marcada (true)
    final JButton dicaBtn = new BotaoCabo("", new Color(255, 210, 60), true); // polimorfismo = variável do tipo JButton guardando um objeto BotaoCabo (subclasse de JButton)
    final JLabel faseLbl = new JLabel(); // rótulo com o nome da fase atual
    final JLabel jogadorLbl = new JLabel(); // rótulo com o nome do jogador
    final JLabel tempoLbl = new JLabel(); // rótulo com o tempo e a quantidade de testes
    final JLabel progresso = new JLabel(); // rótulo com as estrelas de cada fase
    final JLabel status = new JLabel(" "); // barra de mensagens do rodapé (começa com um espaço para já ocupar a altura)
    final Board board = new Board(); // composição = a janela TEM um tabuleiro: objeto da classe interna Board
    String jogador;   // null ate alguem escolher "Novo jogo" no menu (o adm fica como NOME_ADMIN)
    boolean ehAdmin;  // o jogador atual entrou com o login e a senha do adm

    // telas (CardLayout): menu inicial, nome do jogador, ranking e o jogo em si
    static final String MENU = "menu", NOME = "nome", RANKING = "ranking", JOGO = "jogo", RESULTADO = "resultado"; // nomes das telas, usados como chaves do CardLayout
    final CardLayout cartas = new CardLayout(); // CardLayout: mostra um painel por vez, como cartas empilhadas
    final JPanel raiz = new JPanel(cartas); // painel raiz, que contém todas as telas
    final Map<String, Runnable> atalhos = new HashMap<>();   // "tela:tecla" -> acao; coleção + genéricos = Map (interface) implementado por HashMap: liga um texto a uma ação (Runnable)
    String tela = MENU; // tela que está aparecendo agora
    String origemRanking = MENU;   // para onde o "voltar" do ranking leva
    float animMenu = 0; // contador de tempo da animação dos menus
    final TelaMenu telaMenu = new TelaMenu(); // composição = objeto da tela de menu
    final TelaNome telaNome = new TelaNome(); // objeto da tela de nome
    final TelaRanking telaRanking = new TelaRanking(); // objeto da tela de ranking
    final TelaResultado telaResultado = new TelaResultado(); // objeto da tela de resultado

    public RJ45Game() { // construtor = monta a janela inteira, é executado pelo "new RJ45Game()" lá no main
        super("Monte o cabo de rede - RJ45 / Cat5e"); // super = chama o construtor da superclasse JFrame, passando o título da janela

        JButton nova = new BotaoCabo("Nova partida (N)", CORES[1], true); // botão "Nova partida" (laranja)
        JButton embaralhar = new BotaoCabo("Embaralhar fios (R)", CORES[5], true); // botão "Embaralhar fios" (verde)
        JButton ranking = new BotaoCabo("Ranking (K)", CORES[3], true); // botão "Ranking" (azul)
        JButton trocar = new BotaoCabo("Trocar jogador", CORES[1], true); // botão "Trocar jogador"
        JButton menu = new BotaoCabo("Menu inicial (M)", CORES[5], true); // botão "Menu inicial"
        for (JComponent c : new JComponent[]{dicasBox, dicaBtn, nova, embaralhar, ranking, trocar, menu}) // polimorfismo = um vetor de JComponent guarda a caixa de seleção e os botões: todos SÃO JComponent
            c.setFocusable(false);   // assim os atalhos de teclado sempre funcionam

        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4)); // primeira linha da barra do topo, com FlowLayout alinhado à esquerda
        faseLbl.setFont(faseLbl.getFont().deriveFont(Font.BOLD, 17f)); // deriveFont cria uma variação da fonte atual: negrito, tamanho 17
        faseLbl.setForeground(Color.WHITE); // texto branco
        linha1.add(faseLbl); linha1.add(dicaBtn); linha1.add(embaralhar); linha1.add(nova); // adiciona os componentes à linha 1, na ordem em que aparecem
        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2)); // segunda linha da barra do topo
        jogadorLbl.setFont(jogadorLbl.getFont().deriveFont(Font.BOLD)); // nome do jogador em negrito
        jogadorLbl.setForeground(DESTAQUE); // e na cor de destaque
        tempoLbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); // tempo em fonte monoespaçada (os números não "dançam")
        tempoLbl.setForeground(new Color(120, 255, 170)); // verde claro
        dicasBox.setOpaque(false); // caixa sem fundo próprio (transparente)
        linha2.add(jogadorLbl); linha2.add(trocar); linha2.add(ranking); linha2.add(menu); linha2.add(dicasBox); linha2.add(tempoLbl); // adiciona os componentes à linha 2
        JPanel linha3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2)); // terceira linha da barra do topo
        linha3.add(progresso); // que contém o progresso das fases
        // barra do topo: mesmo fundo escuro do menu, com uma linha azul embaixo
        JPanel topo = new JPanel(new GridLayout(3, 1)) { // classe anônima = um JPanel "sob medida": a subclasse é declarada e instanciada no mesmo lugar
            @Override protected void paintComponent(Graphics g) { // sobrescrita = @Override redefine como este painel se desenha
                ((Graphics2D) g).setPaint(new GradientPaint(0, 0, new Color(16, 20, 30), 0, getHeight(), new Color(26, 30, 42))); // cast = converte Graphics em Graphics2D e define um degradê vertical
                g.fillRect(0, 0, getWidth(), getHeight()); // pinta o fundo inteiro
                g.setColor(new Color(80, 200, 255, 90)); // azul translúcido (o 4º número é a opacidade)
                g.fillRect(0, getHeight() - 2, getWidth(), 2); // linha de 2 pixels na base da barra
            }
        }; // fim da classe anônima
        topo.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8)); // margem interna da barra
        for (JPanel l : new JPanel[]{linha1, linha2, linha3}) l.setOpaque(false); // deixa as três linhas transparentes, para aparecer o degradê
        topo.add(linha1); topo.add(linha2); topo.add(linha3); // adiciona as três linhas à barra do topo

        status.setOpaque(true); // o rodapé pinta o próprio fundo
        status.setBackground(new Color(16, 20, 30)); // fundo escuro
        status.setForeground(Color.WHITE); // texto branco
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(58, 64, 82)), // borda composta: uma linha no topo...
                BorderFactory.createEmptyBorder(8, 14, 8, 14))); // ...e uma margem interna
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f)); // negrito, tamanho 14

        dicasBox.addActionListener(e -> board.dicasAlteradas()); // lambda = "e -> ..." é o tratador do evento: ao marcar/desmarcar a caixa, avisa o tabuleiro
        dicaBtn.addActionListener(e -> board.usarDica()); // clique no botão de dica
        nova.addActionListener(e -> board.novaPartida()); // clique em "Nova partida"
        embaralhar.addActionListener(e -> board.embaralharFios()); // clique em "Embaralhar fios"
        ranking.addActionListener(e -> abrirRanking()); // clique em "Ranking"
        trocar.addActionListener(e -> trocarJogador()); // clique em "Trocar jogador"
        menu.addActionListener(e -> voltarAoMenu()); // clique em "Menu inicial"

        atalho(JOGO, "T", board::iniciarTeste); // referência de método = na tela do jogo, a tecla T chama board.iniciarTeste()
        atalho(JOGO, "ENTER", board::iniciarTeste); // Enter também testa
        atalho(JOGO, "R", board::embaralharFios); // R embaralha os fios
        atalho(JOGO, "D", board::usarDica); // D usa uma dica
        atalho(JOGO, "N", board::novaPartida); // N começa uma nova partida
        atalho(JOGO, "K", this::abrirRanking); // K abre o ranking ("this::" = método deste próprio objeto)
        atalho(JOGO, "M", this::voltarAoMenu); // M volta ao menu
        atalho(MENU, "UP", () -> telaMenu.mover(-1)); // seta para cima no menu: seleciona a opção anterior
        atalho(MENU, "DOWN", () -> telaMenu.mover(1)); // seta para baixo no menu: seleciona a próxima opção
        atalho(MENU, "ENTER", () -> telaMenu.escolher(telaMenu.sel)); // Enter no menu confirma a opção selecionada
        atalho(NOME, "ESCAPE", () -> mostrar(MENU)); // Esc na tela de nome volta ao menu
        atalho(RANKING, "ESCAPE", this::fecharRanking); // Esc fecha o ranking
        atalho(RANKING, "ENTER", this::fecharRanking); // Enter também fecha o ranking
        atalho(RESULTADO, "ENTER", () -> telaResultado.principal.run()); // Enter na tela de resultado executa a ação principal
        atalho(RESULTADO, "K", this::abrirRanking); // K na tela de resultado abre o ranking
        atalho(RESULTADO, "M", this::voltarAoMenu); // M na tela de resultado volta ao menu

        JPanel jogo = new JPanel(new BorderLayout()); // painel da tela do jogo, com BorderLayout (norte, centro, sul)
        jogo.add(topo, BorderLayout.NORTH); // barra do topo em cima
        jogo.add(board, BorderLayout.CENTER); // tabuleiro no centro (ocupa o espaço que sobrar)
        jogo.add(status, BorderLayout.SOUTH); // mensagens embaixo
        raiz.add(telaMenu, MENU); // registra cada tela no CardLayout com o seu nome: menu
        raiz.add(telaNome, NOME); // tela de nome
        raiz.add(telaRanking, RANKING); // tela de ranking
        raiz.add(jogo, JOGO); // tela do jogo
        raiz.add(telaResultado, RESULTADO); // tela de resultado
        add(raiz); // coloca o painel raiz dentro da janela (método herdado de JFrame)
        setDefaultCloseOperation(EXIT_ON_CLOSE); // fechar a janela encerra o programa
        pack(); // ajusta o tamanho da janela ao conteúdo
        setMinimumSize(new Dimension(700, 520)); // tamanho mínimo permitido
        setLocationRelativeTo(null); // centraliza a janela na tela
        aplicarPerfil(); // mostra ou esconde o que é exclusivo do adm
        board.novaPartida(); // deixa a primeira partida preparada

        // animacao dos cabos nas telas de menu
        new javax.swing.Timer(30, e -> { // timer + lambda = a cada 30 ms o Swing executa o bloco abaixo
            if (tela.equals(JOGO)) return; // durante o jogo esta animação fica parada
            animMenu += 0.02f; // avança o tempo da animação
            telaMenu.animar(); // move os cabos do menu
            raiz.repaint(); // pede para redesenhar a tela
        }).start(); // liga o temporizador
    }

    /** Atalho de teclado que so vale na tela indicada (a mesma tecla pode ter ações diferentes em cada tela). */
    void atalho(String naTela, String tecla, Runnable acao) { // método que registra um atalho de teclado: tela, nome da tecla e ação
        atalhos.put(naTela + ":" + tecla, acao); // guarda a ação no mapa, com a chave "tela:tecla"
        JRootPane r = getRootPane(); // painel raiz da janela, onde ficam os atalhos globais
        r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla), tecla); // liga a tecla a um nome de ação, valendo com a janela em foco
        r.getActionMap().put(tecla, new AbstractAction() { // classe anônima = subclasse de AbstractAction (classe abstrata do Swing) criada na hora
            public boolean isEnabled() { return atalhos.containsKey(tela + ":" + tecla); } // o atalho só fica ativo se houver ação cadastrada para a tela atual
            public void actionPerformed(ActionEvent e) { // método abstrato implementado = é chamado quando a tecla é pressionada
                Runnable a = atalhos.get(tela + ":" + tecla); // busca no mapa a ação da tela atual
                if (a != null) a.run(); // se existir, executa
            }
        }); // fim da classe anônima e da chamada put(...)
    }

    // navegacao entre as telas 
    void mostrar(String t) { // método que troca a tela visível
        tela = t; // guarda qual é a tela atual
        cartas.show(raiz, t); // pede ao CardLayout para mostrar o painel com esse nome
        if (t.equals(NOME)) telaNome.campo.requestFocusInWindow(); // na tela de nome, o foco do teclado vai para o campo de texto
        else raiz.requestFocusInWindow(); // nas outras telas, vai para o painel raiz
    }

    void novoJogo() { // método que abre a tela de nome
        telaNome.abrir(); // prepara o campo de texto
        mostrar(NOME); // e mostra a tela
    }

    /** Chamado pela tela de nome: comeca uma partida da fase 1 com esse jogador. */
    void entrarNoJogo(String nome, boolean adm) { // método que começa a jogar com o nome informado
        jogador = nome; // guarda o nome do jogador
        ehAdmin = adm; // guarda se ele é o adm
        aplicarPerfil(); // ajusta a interface ao perfil
        board.novaPartida(); // zera a partida
        mostrar(JOGO); // mostra a tela do jogo
    }

    void abrirRanking() { // método que abre a tela de ranking
        if (!tela.equals(RANKING)) origemRanking = tela; // lembra de qual tela veio, para o "voltar" funcionar
        telaRanking.atualizar(); // recarrega os dados do arquivo
        mostrar(RANKING); // mostra a tela
    }

    void fecharRanking() { mostrar(origemRanking); } // método que volta para a tela de onde o ranking foi aberto

    /** Sai do jogo para o menu inicial (a partida em andamento e descartada). */
    void voltarAoMenu() { // método que volta ao menu inicial
        if (board.emAndamento() && JOptionPane.showConfirmDialog(this, // se há partida em andamento, abre um diálogo de confirmação...
                "Voltar ao menu inicial? A partida atual será perdida.", "Menu inicial", // ...com esta mensagem e este título...
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return; // ...e sai do método se a resposta não for "Sim"
        board.novaPartida(); // descarta a partida atual
        mostrar(MENU); // mostra o menu
    }

    boolean admin() { return ehAdmin; } // encapsulamento = as outras classes perguntam por aqui se o jogador é adm

    /** Mostra a opcao "Mostrar dicas" somente para o admin. */
    void aplicarPerfil() { // método que aplica o perfil do jogador à interface
        dicasBox.setVisible(admin()); // a caixa só aparece para o adm
        dicasBox.setSelected(admin()); // e já vem marcada para ele
    }

    static String estrelas(int n) { return "★".repeat(n) + "☆".repeat(3 - n); } // método que monta o texto com n estrelas cheias e o resto vazias (String.repeat repete o texto)

    /** Cor das estrelas de uma fase: 1 = vermelha, 2 = amarela, 3 = verde. */
    static Color corEstrelas(int n) { return n >= 3 ? VERDE : n == 2 ? AMARELO : n == 1 ? VERMELHO : CINZA; } // método que escolhe a cor das estrelas com operador ternário encadeado (condição ? se_sim : se_não)

    static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); } // método que converte uma cor p/ o formato #rrggbb usado no HTML dos rótulos

    /** Estrela de 5 pontas com centro em (cx, cy) e raio r. */
    static Shape estrela(double cx, double cy, double r) { // método que constrói a forma de uma estrelinha
        Path2D p = new Path2D.Double(); // caminho geométrico vazio
        for (int k = 0; k < 10; k++) { // 10 vértices: 5 pontas e 5 reentrâncias
            double a = -Math.PI / 2 + k * Math.PI / 5, raio = k % 2 == 0 ? r : r * 0.42; // ângulo do vértice e raio (cheio nas pontas, 42% nas reentrâncias)
            if (k == 0) p.moveTo(cx + raio * Math.cos(a), cy + raio * Math.sin(a)); // primeiro vértice: apenas posiciona a "caneta"
            else p.lineTo(cx + raio * Math.cos(a), cy + raio * Math.sin(a)); // demais vértices: traça uma linha até eles
        }
        p.closePath(); // fecha o contorno, ligando o último ponto ao primeiro
        return p; // polimorfismo = retorna o Path2D como Shape (interface que ele implementa)
    }

    static void escrever(Graphics2D g, String s, double x, double y, Color c, Font f, boolean centro) { // método que escreve um texto na posição (x, y), com cor e fonte; centro = centralizar em x
        g.setFont(f); // define a fonte
        g.setColor(c); // define a cor
        if (centro) x -= g.getFontMetrics().stringWidth(s) / 2.0; // para centralizar, recua metade da largura do texto
        g.drawString(s, (float) x, (float) y); // desenha o texto
    }

    void jogarNovamente() { // método que começa outra partida com o mesmo jogador
        board.novaPartida(); // zera a partida
        mostrar(JOGO); // volta para a tela do jogo
    }

    /**
     * Dialogo modal no estilo do menu: titulo, um corpo qualquer e as opções em forma de porta de rede.
     * Devolve o indice da opção escolhida, ou -1 se fechar com Esc.
     */
    int perguntar(String titulo, Color corTitulo, JComponent corpo, String[] opcoes, String[] descr, Color[] cores) { // método responsável pelo diálogo bonitinho de perguntas, recebe vetores paralelos: opções, descrições e cores
        JDialog d = new JDialog(this, titulo, true); // JDialog modal (o "true"): bloqueia a janela principal até ser fechado
        d.setUndecorated(true); // sem a barra de título do sistema
        int[] escolha = {-1}; // vetor de 1 posição: variável local usada em lambda não pode ser reatribuída, mas o conteúdo do vetor pode
        JPanel p = new JPanel() { // classe anônima = painel do diálogo, com desenho próprio
            @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho do fundo do diálogo
                Graphics2D g = (Graphics2D) g0.create(); // cria uma cópia do contexto gráfico, para não alterar o original
                suavizar(g); // liga o antisserrilhamento
                int w = getWidth(), h = getHeight(); // largura e altura do painel
                g.setPaint(new GradientPaint(0, 0, new Color(16, 20, 30), 0, h, new Color(34, 39, 52))); // degradê escuro, de cima para baixo
                g.fillRect(0, 0, w, h); // pinta o fundo
                g.setColor(new Color(255, 255, 255, 12)); // branco quase transparente
                for (int y = 14; y < h; y += 28) // laços aninhados = percorre as linhas...
                    for (int x = 14; x < w; x += 28) g.fillRect(x, y, 2, 2); // ...e as colunas, desenhando um pontilhado a cada 28 pixels
                // um par trançado no topo, como nas telas de menu
                for (int lado = 0; lado < 2; lado++) { // dois fios, um de cada lado do par trançado
                    Path2D par = new Path2D.Double(); // caminho do fio
                    for (int x = -10; x <= w + 10; x += 6) { // percorre a largura de 6 em 6 pixels
                        double y = 20 + (lado == 0 ? 7 : -7) * Math.sin(x / 30.0); // altura em forma de onda (seno); os dois fios ficam em oposição
                        if (x == -10) par.moveTo(x, y); else par.lineTo(x, y); // o primeiro ponto move a caneta, os demais traçam linhas
                    }
                    pintarCabo(g, par, corTitulo, 5, lado == 1); // pinta o fio (o segundo é o listrado)
                }
                g.setStroke(new BasicStroke(2)); // espessura do traço 2
                g.setColor(BORDA); // cor da borda
                g.drawRect(1, 1, w - 2, h - 2); // contorno do diálogo
                g.dispose(); // libera a cópia do contexto gráfico
            }
        }; // fim da classe anônima
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); // BoxLayout no eixo Y: componentes empilhados na vertical
        p.setBorder(BorderFactory.createEmptyBorder(52, 34, 26, 34)); // margens internas (em cima, esquerda, embaixo, direita)
        JLabel t = new JLabel(titulo); // rótulo do título
        t.setFont(t.getFont().deriveFont(Font.BOLD, 26f)); // fonte grande em negrito
        t.setForeground(corTitulo); // na cor recebida por parâmetro
        t.setAlignmentX(LEFT_ALIGNMENT); // alinhado à esquerda
        corpo.setAlignmentX(LEFT_ALIGNMENT); // o corpo também fica alinhado à esquerda
        p.add(t); // adiciona o título
        p.add(Box.createVerticalStrut(14)); // espaço vertical de 14 pixels
        p.add(corpo); // adiciona o corpo
        p.add(Box.createVerticalStrut(20)); // espaço vertical de 20 pixels
        for (int i = 0; i < opcoes.length; i++) { // cria um botão para cada opção
            int indice = i; // cópia do índice: a lambda só pode usar variável local que não muda
            OpcaoPorta b = new OpcaoPorta(opcoes[i], descr[i], cores[i]); // instanciação = cria o botão em forma de porta de rede
            b.setAlignmentX(LEFT_ALIGNMENT); // alinhado à esquerda
            b.addActionListener(e -> { escolha[0] = indice; d.dispose(); }); // lambda = ao clicar: guarda a opção escolhida e fecha o diálogo
            if (i > 0) p.add(Box.createVerticalStrut(10)); // espaço entre uma opção e outra (menos antes da primeira)
            p.add(b); // adiciona o botão
        }
        JLabel rodape = new JLabel("↑ ↓ para escolher   ·   Enter para confirmar"); // rodapé com as instruções de teclado
        rodape.setFont(rodape.getFont().deriveFont(Font.PLAIN, 12f)); // fonte normal, tamanho 12
        rodape.setForeground(CINZA); // cinza
        rodape.setAlignmentX(LEFT_ALIGNMENT); // alinhado à esquerda
        p.add(Box.createVerticalStrut(14)); // espaço vertical
        p.add(rodape); // adiciona o rodapé

        JRootPane r = d.getRootPane(); // painel raiz do diálogo
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager(); // gerenciador do foco do teclado
        r.registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW); // Esc fecha o diálogo sem escolher nada
        r.registerKeyboardAction(e -> kfm.focusNextComponent(), KeyStroke.getKeyStroke("DOWN"), JComponent.WHEN_IN_FOCUSED_WINDOW); // seta para baixo: foco no próximo botão
        r.registerKeyboardAction(e -> kfm.focusPreviousComponent(), KeyStroke.getKeyStroke("UP"), JComponent.WHEN_IN_FOCUSED_WINDOW); // seta para cima: foco no botão anterior
        d.setContentPane(p); // define o painel como o conteúdo do diálogo
        d.pack(); // ajusta o tamanho ao conteúdo
        d.setLocationRelativeTo(this); // centraliza sobre a janela do jogo
        d.setVisible(true);   // modal: so volta quando escolher
        return escolha[0]; // retorna a opção escolhida (ou -1)
    }

    /** Troca de jogador sempre pela tela de nome (e la que o adm digita a senha). */
    void trocarJogador() { // método que troca de jogador
        if (board.emAndamento() && JOptionPane.showConfirmDialog(this, // se há partida em andamento, pede confirmação...
                "Trocar de jogador? A partida atual será perdida.", "Trocar jogador", // ...com esta mensagem e este título...
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return; // ...e sai do método se a resposta não for "Sim"
        board.novaPartida(); // descarta a partida atual
        novoJogo(); // abre a tela de nome
    }

    /** Tira do nome o que nao pode ir para o CSV do ranking. Pode devolver vazio. */
    static String limparNome(String s) { // método que limpa o nome digitado
        s = s.replace(";", "").trim(); // remove ";" (separador do CSV) e os espaços das pontas
        return s.length() > 20 ? s.substring(0, 20) : s; // corta em 20 caracteres, se for maior
    }

    static String semHtml(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); } // método que troca & e < pelos códigos deles, p/ o nome não ser interpretado como HTML

    // ranking (arquivo CSV na pasta do usuario, uma linha por partida completa) 
    /** fases: um caractere por fase, '1' = acertou, '0' = pulou. */
    record Resultado(String nome, int pontos, int tempo, int testes, String fases, boolean dicas, String padrao, String data) { // record = classe de dados imutável: o Java gera sozinho o construtor, os métodos de acesso, equals, hashCode e toString
        String linha() { // método que transforma o resultado em uma linha de texto do CSV
            return String.join(";", nome, "" + pontos, "" + tempo, "" + testes, fases, dicas ? "1" : "0", padrao, data); // junta os campos com ";" (os números viram texto com "" + valor)
        }
        static Resultado ler(String l) { // método estático de fábrica = cria um Resultado a partir de uma linha do arquivo
            String[] p = l.split(";"); // separa os campos pelo ";"
            String fases = p[4]; // campo com a situação das fases
            if (fases.length() == 1) fases = fases + "1";   // linha da versao de 2 fases (RJ45 + keystone)
            // partidas antigas do adm foram gravadas com o login: mostra sempre o nome publico
            String nome = loginAdm(p[0]) ? NOME_ADMIN : p[0]; // ternário = se o nome gravado for o login do adm, usa o nome público
            return new Resultado(nome, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), // cria o objeto, convertendo os textos em números com Integer.parseInt...
                    fases, p[5].equals("1"), p[6], p[7]); // ...e "1"/"0" em boolean
        }
        String fasesTexto() { // método que monta o texto das fases p/ a tabela: um certinho ou xizinho por fase
            StringBuilder sb = new StringBuilder(); // montador de texto
            for (char c : fases.toCharArray()) sb.append(c == '1' ? "✔ " : "✗ "); // percorre cada caractere do código das fases
            return sb.toString().trim(); // retorna o texto sem o espaço do final
        }
    } // fim do record Resultado

    static List<Resultado> carregarRanking() { // método que lê o arquivo do ranking e retorna a lista já ordenada
        List<Resultado> lista = new ArrayList<>(); // polimorfismo = variável do tipo List (interface) com um objeto ArrayList (implementação)
        try { // try = a leitura de arquivo pode falhar
            if (Files.exists(ARQ_RANKING)) // só lê se o arquivo existir
                for (String l : Files.readAllLines(ARQ_RANKING, StandardCharsets.UTF_8)) { // lê todas as linhas e percorre uma a uma
                    try { lista.add(Resultado.ler(l)); } catch (RuntimeException ignorada) { /* linha corrompida */ } // converte a linha em Resultado, se ela estiver corrompida a exceção é capturada e a linha é ignorada X_X
                }
        } catch (IOException e) { // catch = erro de leitura do arquivo
            System.err.println("Não consegui ler o ranking: " + e.getMessage()); // avisa na saída de erro do terminal
        }
        lista.sort(Comparator.comparingInt(Resultado::pontos).reversed().thenComparingInt(Resultado::tempo)); // comparator = ordena por pontos (maior primeiro) e, no empate, pelo menor tempo
        return lista; // retorna a lista ordenada
    }

    static void salvarResultado(Resultado r) { // método que grava um resultado no fim do arquivo
        try { // a gravação pode falhar
            Files.writeString(ARQ_RANKING, r.linha() + System.lineSeparator(), StandardCharsets.UTF_8, // escreve a linha seguida de uma quebra de linha...
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND); // ...criando o arquivo se ele não existir (CREATE) e acrescentando ao final (APPEND)
        } catch (IOException e) { // erro de gravação
            System.err.println("Não consegui salvar o ranking: " + e.getMessage()); // avisa no terminal
        }
    }

    /**
     * Apaga do arquivo as partidas que batem com o filtro (só a primeira, se soUma). Antes de regravar,
     * guarda uma copia em ".bak". Devolve quantas linhas saiu, ou -1 se nao conseguiu gravar.
     */
    static int removerDoRanking(java.util.function.Predicate<Resultado> alvo, boolean soUma) { // método que remove partidas do ranking. Recebe um Predicate (interface funcional) = uma função que responde true/false p/ cada resultado
        try { // operações com arquivo ficam dentro de try
            if (!Files.exists(ARQ_RANKING)) return 0; // sem arquivo não há o que remover
            List<String> ficam = new ArrayList<>(); // linhas que vão continuar no arquivo
            int removidas = 0; // contador de linhas removidas
            for (String l : Files.readAllLines(ARQ_RANKING, StandardCharsets.UTF_8)) { // percorre cada linha do arquivo
                boolean tira = false; // por padrão, a linha fica
                if (!(soUma && removidas > 0)) { // se era para tirar só uma e ela já saiu, não testa mais
                    try { tira = alvo.test(Resultado.ler(l)); } catch (RuntimeException ignorada) { /* linha corrompida: fica */ } // aplica o teste (Predicate) ao resultado desta linha
                }
                if (tira) removidas++; else ficam.add(l); // conta a linha removida, ou guarda a linha que fica
            }
            if (removidas > 0) { // só regrava o arquivo se removeu alguma coisa
                Files.copy(ARQ_RANKING, ARQ_RANKING.resolveSibling(ARQ_RANKING.getFileName() + ".bak"), // antes, copia o arquivo para ".bak"...
                        StandardCopyOption.REPLACE_EXISTING); // ...substituindo a cópia anterior
                Files.write(ARQ_RANKING, ficam, StandardCharsets.UTF_8); // regrava o arquivo só com as linhas que ficam
            }
            return removidas; // retorna quantas linhas saíram
        } catch (IOException e) { // erro ao ler ou gravar
            System.err.println("Não consegui regravar o ranking: " + e.getMessage()); // avisa no terminal
            return -1; // -1 sinaliza o erro para quem chamou
        }
    }

    static String formatarTempo(int seg) { return String.format("%02d:%02d", seg / 60, seg % 60); } // método que formata segundos como mm:ss (divisão inteira e resto)

    //  desenho compartilhado pelas telas (tema: cabos de rede) 
    static void suavizar(Graphics2D g) { // método que liga as opções de qualidade do desenho
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // suaviza as bordas das formas (antisserrilhamento)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // suaviza as letras
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE); // traços posicionados com mais precisão
    }

    /** Um cabo/fio ao longo do caminho: contorno escuro, cor e (se listrado) branco com tracos da cor. */
    static void pintarCabo(Graphics2D g, Shape caminho, Color cor, float esp, boolean listrado) { // método que desenha um cabo ou fio ao longo de qualquer forma (Shape)
        Stroke old = g.getStroke(); // guarda o traço atual, para restaurar no final
        g.setStroke(new BasicStroke(esp + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // traço 3 pixels mais grosso, com pontas e junções arredondadas
        g.setColor(new Color(0, 0, 0, 110)); // preto translúcido
        g.draw(caminho); // desenha o contorno escuro
        g.setStroke(new BasicStroke(esp, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // traço na espessura do fio
        g.setColor(listrado ? Color.WHITE : cor); // branco se for listrado; senão, a cor do fio
        g.draw(caminho); // desenha o fio
        if (listrado) { // fio listrado:
            g.setStroke(new BasicStroke(esp, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{9, 13}, 0)); // traço tracejado (9 pixels desenhados, 13 pulados)
            g.setColor(cor); // na cor do fio
        } else { // fio de cor inteira:
            g.setStroke(new BasicStroke(esp * 0.22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // traço fino
            g.setColor(new Color(255, 255, 255, 70)); // branco translúcido, para dar brilho
        }
        g.draw(caminho); // desenha as listras ou o brilho
        g.setStroke(old); // restaura o traço original
    }

    /** Plugue RJ45 visto de lado, apontando para a direita. (fx, cy) = ponta da frente; s = escala. Ocupa 60*s para tras. */
    static void pintarPlugue(Graphics2D g0, double fx, double cy, double s, Color cabo) { // método que desenha um plugue RJ45
        Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico (as transformações abaixo não "vazam")
        g.translate(fx, cy); // move a origem do desenho para a ponta do plugue
        g.scale(s, s); // aplica a escala
        // capa de borracha na saida do cabo
        g.setColor(cabo.darker()); // cor do cabo, mais escura
        g.fill(new RoundRectangle2D.Double(-60, -9, 28, 18, 8, 8)); // retângulo arredondado da capa
        // trava
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // traço de 3 pixels com pontas arredondadas
        g.setColor(new Color(170, 190, 210)); // cinza azulado
        g.draw(new Line2D.Double(-34, -15, -10, -13)); // linha da trava
        // corpo transparente com os fios aparecendo
        Shape corpo = new RoundRectangle2D.Double(-36, -12, 36, 24, 5, 5); // polimorfismo = o RoundRectangle2D é guardado como Shape
        g.setColor(new Color(214, 230, 244)); // cor do plástico
        g.fill(corpo); // preenche o corpo
        int[] par = {1, 5, 3, 7};   // laranja, verde, azul, marrom
        for (int k = 0; k < 4; k++) { // percorre cada um dos 4 pares
            g.setColor(CORES[par[k]]); // cor do par
            g.fill(new Rectangle2D.Double(-34, -9 + k * 5, 20, 3)); // pequeno retângulo representando o fio
        }
        // contatos dourados
        g.setColor(new Color(235, 190, 40)); // dourado
        for (int k = 0; k < 4; k++) g.fill(new Rectangle2D.Double(-13, -9 + k * 5, 11, 3)); // 4 contatos metálicos
        g.setStroke(new BasicStroke(1)); // traço fino
        g.setColor(new Color(110, 122, 140)); // cinza
        g.draw(corpo); // contorno do corpo
        g.dispose(); // libera a cópia do contexto gráfico
    }

    /** Fundo das telas de menu: gradiente escuro, pontilhado e 4 pares trancados passando em cima e embaixo. */
    static void pintarFundo(Graphics2D g, int w, int h, float t) { // método que pinta o fundo animado das telas de menu; t = tempo da animação
        g.setPaint(new GradientPaint(0, 0, new Color(16, 20, 30), 0, h, new Color(34, 39, 52))); // degradê escuro, de cima para baixo
        g.fillRect(0, 0, w, h); // pinta a tela inteira
        g.setColor(new Color(255, 255, 255, 12)); // branco quase transparente
        for (int y = 14; y < h; y += 28) // percorre as linhas...
            for (int x = 14; x < w; x += 28) g.fillRect(x, y, 2, 2); // ...e as colunas do pontilhado

        double[] ys = {0.05, 0.13, 0.89, 0.96}; // altura relativa (0 a 1) de cada um dos 4 pares
        int[] par = {1, 5, 3, 7};   // laranja, verde, azul, marrom
        Composite old = g.getComposite(); // guarda o modo de composição atual
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f)); // passa a desenhar com 55% de opacidade
        for (int k = 0; k < 4; k++) { // percorre cada par
            double y0 = h * ys[k], fase = t * (k % 2 == 0 ? 0.8 : -0.8) + k * 1.7; // altura base e deslocamento da onda (pares alternados andam em sentidos opostos)
            for (int lado = 0; lado < 2; lado++) { // os dois fios do par
                Path2D p = new Path2D.Double(); // caminho do fio
                for (int x = -10; x <= w + 10; x += 6) { // percorre a largura da tela
                    double y = y0 + (lado == 0 ? 13 : -13) * Math.sin(x / 46.0 + fase); // onda senoidal; os dois fios ficam em oposição, formando a trança
                    if (x == -10) p.moveTo(x, y); else p.lineTo(x, y); // o primeiro ponto move a caneta, os demais traçam linhas
                }
                pintarCabo(g, p, CORES[par[k]], 7, lado == 1); // pinta o fio (o segundo do par é o listrado)
            }
        }
        g.setComposite(old); // restaura a opacidade normal
    }

    /** Botao escuro arredondado com um LED que acende quando o mouse passa (usado nas telas de nome e ranking). */
    static class BotaoCabo extends JButton { // herança + classe aninhada estática = BotaoCabo É um JButton, só que com visual próprio
        final Color cor; // atributo final = cor da borda quando o botão está aceso
        final boolean compacto;   // versao menor, para a barra do topo do jogo
        BotaoCabo(String texto, Color cor) { this(texto, cor, false); } // sobrecarga: construtor de 2 parâmetros: chama o de 3 com this(...), usando compacto = false
        BotaoCabo(String texto, Color cor, boolean compacto) { // construtor completo
            super(texto); // super = chama o construtor de JButton, que guarda o texto
            this.cor = cor; // this = diferencia o atributo "cor" do parâmetro "cor"
            this.compacto = compacto; // guarda o tamanho escolhido
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false); // desliga o desenho padrão do botão (fundo, borda, foco)
            setRolloverEnabled(true); // permite saber quando o mouse está por cima
            setForeground(Color.WHITE); // texto branco
            setFont(getFont().deriveFont(Font.BOLD, compacto ? 12.5f : 15f)); // o tamanho da fonte depende de "compacto"
            setBorder(compacto ? BorderFactory.createEmptyBorder(6, 28, 6, 14) : BorderFactory.createEmptyBorder(11, 40, 11, 24)); // as margens internas também
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cursor de mãozinha
        }
        @Override protected void paintComponent(Graphics g0) { // sobrescrita = redefine o método de desenho herdado de JButton
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            boolean aceso = isEnabled() && (getModel().isRollover() || getModel().isPressed() || isFocusOwner()); // "aceso": habilitado e com o mouse por cima, pressionado ou com o foco
            int arco = compacto ? 12 : 16, led = compacto ? 8 : 10; // arredondamento dos cantos e tamanho do LED
            int w = getWidth(), h = getHeight(); // largura e altura do botão
            g.setPaint(new GradientPaint(0, 0, new Color(56, 62, 80), 0, h, new Color(40, 45, 58))); // degradê cinza-azulado
            g.fillRoundRect(1, 1, w - 3, h - 3, arco, arco); // fundo arredondado
            g.setStroke(new BasicStroke(aceso ? 2.2f : 1.2f)); // borda mais grossa quando aceso
            g.setColor(aceso ? cor : isEnabled() ? BORDA : new Color(64, 70, 86)); // cor da borda: a do botão, a normal ou a de desabilitado
            g.drawRoundRect(1, 1, w - 3, h - 3, arco, arco); // contorno
            g.setColor(aceso ? new Color(0, 230, 100) : new Color(78, 84, 98)); // LED verde quando aceso, cinza quando apagado
            g.fillOval(compacto ? 12 : 17, h / 2 - led / 2, led, led); // círculo do LED
            g.dispose(); // libera a cópia
            super.paintComponent(g0); // super.método() = chama o desenho original de JButton, que escreve o texto
        }
    } // fim da classe BotaoCabo

    /** Opcao de dialogo desenhada como as portas do menu inicial: LED, titulo e uma linha explicando. */
    static class OpcaoPorta extends JButton { // herança = outra subclasse de JButton: a opção em forma de porta de rede
        final String descr; // linha de explicação abaixo do título
        final Color cor; // cor de destaque da opção
        OpcaoPorta(String titulo, String descr, Color cor) { // construtor
            super(titulo); // super = o título vai para o JButton
            this.descr = descr; // guarda a descrição
            this.cor = cor; // guarda a cor
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false); // desliga o desenho padrão do botão
            setRolloverEnabled(true); // permite saber quando o mouse está por cima
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cursor de mãozinha
            Dimension t = new Dimension(480, 62); // tamanho fixo de 480 x 62
            setPreferredSize(t); setMinimumSize(t); setMaximumSize(t); // usa o mesmo tamanho como preferido, mínimo e máximo
            // Enter aciona a opcao com foco (no Swing padrao so a barra de espaco faz isso)
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "pressed"); // Enter pressionado = botão pressionado
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released ENTER"), "released"); // Enter solto = botão solto (dispara o clique)
        }
        @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho da opção
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            boolean aceso = getModel().isRollover() || isFocusOwner(); // aceso com o mouse por cima ou com o foco do teclado
            int w = getWidth(), h = getHeight(); // largura e altura
            if (aceso) { // se estiver aceso
                g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 45)); // usa a cor da opção, translúcida
                g.fillRoundRect(0, 0, w, h, 22, 22); // e desenha um halo em volta
            }
            g.setPaint(new GradientPaint(0, 0, new Color(56, 62, 80), 0, h, new Color(38, 43, 56))); // degradê do corpo
            g.fillRoundRect(4, 4, w - 8, h - 8, 16, 16); // corpo arredondado
            g.setColor(new Color(16, 18, 24)); // quase preto
            g.fillRoundRect(9, h / 2 - 15, 8, 30, 4, 4);   // entrada do plugue
            g.setStroke(new BasicStroke(aceso ? 2.4f : 1.2f)); // borda mais grossa quando aceso
            g.setColor(aceso ? cor : BORDA); // cor da borda
            g.drawRoundRect(4, 4, w - 8, h - 8, 16, 16); // contorno
            g.setColor(aceso ? VERDE : new Color(78, 84, 98)); // LED verde ou apagado
            g.fillOval(30, h / 2 - 6, 12, 12); // círculo do LED
            Font base = new Font(Font.DIALOG, Font.BOLD, 17); // fonte base
            escrever(g, getText(), 56, h / 2 - 1, Color.WHITE, base, false); // título da opção (getText() é herdado de JButton)
            escrever(g, descr, 56, h / 2 + 17, new Color(165, 174, 194), base.deriveFont(Font.PLAIN, 12.5f), false); // descrição, em fonte menor
            escrever(g, "›", w - 32, h / 2 + 10, aceso ? cor : BORDA, base.deriveFont(30f), false); // seta à direita
            g.dispose(); // libera a cópia
        }
    } // fim da classe OpcaoPorta

    /** Fileira com os 8 pinos do ultimo teste: verde = certo, vermelho = errado. */
    static class PinosTeste extends JComponent { // herança = componente próprio, herdando direto de JComponent
        final boolean[] ok; // resultado de cada um dos 8 pinos
        PinosTeste(boolean[] ok) { // construtor
            this.ok = ok; // guarda o vetor recebido
            Dimension t = new Dimension(480, 78); // tamanho fixo
            setPreferredSize(t); setMinimumSize(t); setMaximumSize(t); // preferido, mínimo e máximo iguais
        }
        @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho do componente
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            int w = getWidth(), h = getHeight(); // largura e altura
            g.setPaint(new GradientPaint(0, 0, new Color(52, 58, 75), 0, h, new Color(38, 43, 56))); // degradê do fundo
            g.fillRoundRect(0, 0, w - 1, h - 1, 16, 16); // fundo arredondado
            g.setColor(BORDA); // cor da borda
            g.drawRoundRect(0, 0, w - 1, h - 1, 16, 16); // contorno
            Font f = new Font(Font.DIALOG, Font.BOLD, 13); // fonte base
            for (int i = 0; i < 8; i++) { // percorre cada um dos 8 pinos
                int cx = 37 + i * 58; // posição horizontal do pino i
                Color c = ok[i] ? VERDE : VERMELHO; // verde se certo, vermelho se errado
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 50)); // a mesma cor, translúcida
                g.fillOval(cx - 14, 10, 28, 28); // halo em volta do LED
                g.setColor(c); // cor cheia
                g.fillOval(cx - 9, 15, 18, 18); // LED
                escrever(g, "pino " + (i + 1), cx, 56, CLARO, f.deriveFont(Font.PLAIN, 11.5f), true); // rótulo "pino N" (i + 1, porque o vetor começa em 0)
                escrever(g, ok[i] ? "certo" : "errado", cx, 70, c, f.deriveFont(11f), true); // "certo" ou "errado"
            }
            g.dispose(); // libera a cópia
        }
    } // fim da classe PinosTeste

    /** Tela de fim de partida (concluiu ou desistiu): estrelas e pontos por fase, resumo e total. */
    class TelaResultado extends JPanel { // classe interna = sem "static": cada TelaResultado pertence a uma janela RJ45Game e enxerga os atributos e métodos dela
        static final int W = 1000, H = 490;   // tamanho logico do placar
        Fase[] fases = new Fase[0]; // vetor de objetos = fases da partida mostrada; começa vazio
        boolean desistiu, modoAdm, usouDicas; // como a partida terminou
        int seg, testes, dicas, pontos, recorde; // números da partida
        Runnable principal = () -> { };   // acao do Enter (primeiro botao)
        final JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0)); // painel dos botões, centralizados

        TelaResultado() { // construtor
            setLayout(new BorderLayout(0, 12)); // BorderLayout com 12 pixels de espaço vertical
            setBorder(BorderFactory.createEmptyBorder(118, 20, 112, 20)); // margens internas
            JComponent placar = new JComponent() { // classe anônima = componente cujo desenho é o placar
                @Override protected void paintComponent(Graphics g) { pintarPlacar((Graphics2D) g.create(), getWidth(), getHeight()); } // sobrescrita = repassa o desenho para o método pintarPlacar
            }; // fim da classe anônima
            botoes.setOpaque(false); // painel de botões transparente
            add(placar, BorderLayout.CENTER); // placar no centro
            add(botoes, BorderLayout.SOUTH); // botões embaixo
        }

        /** Guarda os numeros da partida que acabou e monta os botoes (quem desistiu ve "Outro jogador" primeiro). */
        void abrir(Fase[] fases, boolean desistiu, int seg, int testes, int dicas, boolean modoAdm, boolean usouDicas, // método que recebe todos os dados da partida que terminou
                   int pontos, int recorde) { // (continuação dos parâmetros)
            this.fases = fases; this.desistiu = desistiu; this.seg = seg; this.testes = testes; this.dicas = dicas; // this = copia cada parâmetro para o atributo de mesmo nome
            this.modoAdm = modoAdm; this.usouDicas = usouDicas; this.pontos = pontos; this.recorde = recorde; // idem
            botoes.removeAll(); // remove os botões da partida anterior
            BotaoCabo de_novo = botao("Jogar novamente", CORES[5], RJ45Game.this::jogarNovamente); // RJ45Game.this:: = referência a um método do objeto da classe externa
            BotaoCabo outro = botao(desistiu ? "Outro jogador" : "Trocar jogador", CORES[1], RJ45Game.this::novoJogo); // o texto do botão depende de ter desistido ou não
            botoes.add(desistiu ? outro : de_novo); // quem desistiu vê "Outro jogador" primeiro
            botoes.add(desistiu ? de_novo : outro); // e o outro botão em seguida
            botoes.add(botao("Ranking (K)", CORES[3], RJ45Game.this::abrirRanking)); // botão do ranking
            botoes.add(botao("Menu inicial (M)", CINZA, RJ45Game.this::voltarAoMenu)); // botão do menu
            BotaoCabo primeiro = (BotaoCabo) botoes.getComponent(0); // cast = getComponent retorna Component; aqui se sabe que é um BotaoCabo
            primeiro.setText(primeiro.getText() + " (Enter)"); // acrescenta "(Enter)" ao texto do primeiro botão
            principal = desistiu ? RJ45Game.this::novoJogo : RJ45Game.this::jogarNovamente; // a ação do Enter acompanha o primeiro botão
            botoes.revalidate(); // manda o painel recalcular o layout
        }

        BotaoCabo botao(String texto, Color cor, Runnable acao) { // método auxiliar que cria um botão que executa a ação recebida
            BotaoCabo b = new BotaoCabo(texto, cor); // cria o botão
            b.setFocusable(false); // sem foco de teclado
            b.addActionListener(e -> acao.run()); // ao clicar, roda a ação (Runnable)
            return b; // retorna o botão pronto
        }

        void pintarPlacar(Graphics2D g, int w, int h) { // método responsável por desenhar o placar
            suavizar(g); // antisserrilhamento
            double escala = Math.min(w / (double) W, h / (double) H); // fator de escala para caber na janela, mantendo a proporção
            g.translate((w - W * escala) / 2, (h - H * escala) / 2); // centraliza o placar
            g.scale(escala, escala); // aplica a escala: daqui para frente as coordenadas são as lógicas (1000 x 490)
            Font base = new Font(Font.DIALOG, Font.PLAIN, 14), negrito = base.deriveFont(Font.BOLD); // fontes normal e negrito
            Font mono = new Font(Font.MONOSPACED, Font.BOLD, 11); // fonte monoespaçada
            Color apagado = new Color(140, 150, 172); // cor de texto apagado

            boolean tudoCerto = true; // começa supondo que todas as fases foram concluídas
            for (Fase f : fases) tudoCerto &= f.ok; // for-each com &= : fica false se alguma fase não estiver ok
            escrever(g, desistiu ? "VOCÊ DESISTIU" : "PARTIDA CONCLUÍDA!", 500, 34, desistiu ? AMARELO : VERDE, negrito.deriveFont(32f), true); // título: desistiu (amarelo) ou concluiu (verde)
            escrever(g, jogador + "  ·  " + (desistiu ? "sua pontuação ficou com o que você fez até aqui" // subtítulo: nome do jogador e uma frase conforme o caso...
                    : tudoCerto ? "rede completa, internet funcionando!" : "você chegou ao fim, mas pulou alguma fase"), // ...(ternários encadeados escolhem a frase)...
                    500, 60, new Color(165, 174, 194), base, true); // ...posição, cor e fonte do subtítulo

            // um cartao por fase: estrelas (vermelha, amarela ou verde), tempo, erros e pontos
            for (int i = 0; i < fases.length; i++) { // percorre cada fase
                Fase f = fases[i]; // fase i
                int x = 120 + i * 260, y = 78, cx = x + 120; // posição do cartão e centro horizontal dele
                Color c = f.ok ? corEstrelas(f.estrelas) : BORDA; // cor conforme as estrelas, ou cinza se a fase não foi concluída
                g.setPaint(new GradientPaint(0, y, new Color(52, 58, 75), 0, y + 160, new Color(38, 43, 56))); // degradê do cartão
                g.fillRoundRect(x, y, 240, 160, 20, 20); // fundo do cartão
                g.setStroke(new BasicStroke(f.ok ? 2.2f : 1.2f)); // borda mais grossa se a fase foi concluída
                g.setColor(c); // cor da borda
                g.drawRoundRect(x, y, 240, 160, 20, 20); // contorno
                escrever(g, "FASE " + (i + 1), cx, y + 24, DESTAQUE, mono, true); // "FASE 1", "FASE 2"...
                escrever(g, FASES[i], cx, y + 46, Color.WHITE, negrito.deriveFont(16f), true); // nome da fase
                if (f.ok) { // fase concluída:
                    for (int k = 0; k < 3; k++) { // desenha as 3 estrelas
                        Shape e = estrela(cx - 38 + k * 38, y + 78, 16); // forma da estrela k
                        g.setStroke(new BasicStroke(1.6f)); // espessura do contorno
                        if (k < f.estrelas) { g.setColor(c); g.fill(e); } // estrela ganha: preenchida
                        else { g.setColor(new Color(96, 104, 124)); g.draw(e); } // estrela não ganha: só o contorno
                    }
                    escrever(g, formatarTempo(f.seg) + "   ·   " + f.falhas + (f.falhas == 1 ? " erro" : " erros"), cx, y + 116, CLARO, base.deriveFont(13f), true); // tempo e número de erros (singular/plural pelo ternário)
                } else { // fase não concluída:
                    escrever(g, f.desistiu ? "DESISTIU" : "PULOU", cx, y + 86, apagado, negrito.deriveFont(20f), true); // "DESISTIU" ou "PULOU"
                    escrever(g, f.falhas + (f.falhas == 1 ? " teste com erro" : " testes com erro"), cx, y + 116, apagado, base.deriveFont(13f), true); // quantos testes com erro
                }
                escrever(g, f.pontos + " pts", cx, y + 146, f.ok ? Color.WHITE : apagado, negrito.deriveFont(20f), true); // pontos da fase
            }

            // resumo da partida
            g.setPaint(new GradientPaint(0, 256, new Color(52, 58, 75), 0, 340, new Color(38, 43, 56))); // degradê da faixa de resumo
            g.fillRoundRect(120, 256, 760, 84, 20, 20); // fundo da faixa
            g.setStroke(new BasicStroke(1.2f)); // traço fino
            g.setColor(BORDA); // cor da borda
            g.drawRoundRect(120, 256, 760, 84, 20, 20); // contorno da faixa
            String[] rotulo = {"TEMPO TOTAL", "TESTES FEITOS", "DICAS USADAS", "BÔNUS SEM DICAS"}; // rótulos das 4 colunas
            String[] valor = {formatarTempo(seg), "" + testes, dicas + (modoAdm ? " + adm" : ""), usouDicas ? "não" : "pontos x2"}; // valores das 4 colunas
            Color[] corValor = {Color.WHITE, Color.WHITE, usouDicas ? AMARELO : VERDE, usouDicas ? apagado : VERDE}; // cores dos valores
            for (int k = 0; k < 4; k++) { // percorre cada coluna
                int cx = 215 + k * 190; // centro da coluna
                if (k > 0) { g.setColor(new Color(96, 104, 124, 140)); g.drawLine(cx - 95, 270, cx - 95, 326); } // linha divisória (menos antes da primeira)
                escrever(g, rotulo[k], cx, 282, new Color(165, 174, 194), mono, true); // rótulo
                escrever(g, valor[k], cx, 318, corValor[k], negrito.deriveFont(24f), true); // valor
            }

            // total: a conta aparece por extenso para dar para entender de onde veio
            StringBuilder conta = new StringBuilder(); // montador do texto da conta
            for (int i = 0; i < fases.length; i++) conta.append(i > 0 ? " + " : "").append(fases[i].pontos); // soma dos pontos das fases: "a + b + c"
            if (!usouDicas) conta.insert(0, "(").append(")  x 2 sem dicas"); // sem dicas: coloca parênteses e o "x 2"
            escrever(g, conta + "  =", 500, 368, new Color(165, 174, 194), mono.deriveFont(13f), true); // escreve a conta
            escrever(g, pontos + " PONTOS", 500, 420, Color.WHITE, negrito.deriveFont(46f), true); // total de pontos, bem grande
            if (pontos > recorde) // se bateu o recorde
                escrever(g, recorde > 0 ? "Novo recorde pessoal!  (antes: " + recorde + ")" : "Primeiro recorde!", 500, 448, AMARELO, negrito.deriveFont(15f), true); // avisa (com o recorde anterior, se havia)
            else // senão
                escrever(g, "seu recorde: " + recorde, 500, 448, apagado, base, true); // mostra o recorde atual
            escrever(g, "Resultado salvo no ranking." + (modoAdm ? "   (adm = opção \"Mostrar dicas\" ligada)" : ""), 500, 474, apagado, base.deriveFont(12f), true); // linha final
            g.dispose(); // libera o contexto gráfico
        }

        @Override protected void paintComponent(Graphics g0) { // sobrescrita = fundo da tela de resultado
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            pintarFundo(g, getWidth(), getHeight(), animMenu); // fundo animado, o mesmo dos menus
            g.dispose(); // libera a cópia
        }
    } // fim da classe TelaResultado

    /** Menu inicial: cada opcao e uma porta de rede; o cabo da opcao escolhida se conecta nela. */
    class TelaMenu extends JPanel { // classe interna + herança = o menu inicial É um JPanel
        static final int W = 1000, H = 780;   // tamanho logico da tela
        final String[] itens = {"NOVO JOGO", "RANKING", "SAIR"}; // textos das opções
        final String[] descr = {"digite seu nome e comece a montar os cabos", "veja as melhores partidas", "fecha o jogo"}; // descrição de cada opção
        final Color[] cor = {CORES[1], CORES[3], CORES[5]};   // laranja, azul, verde
        final float[] plug = new float[3];   // 0 = cabo solto, 1 = conectado na porta
        int sel = 0; // opção selecionada
        double escala = 1, dx = 0, dy = 0; // escala e deslocamento para ajustar o desenho ao tamanho da janela

        TelaMenu() { // construtor
            setPreferredSize(new Dimension(W, H)); // tamanho preferido da tela
            MouseAdapter m = new MouseAdapter() { // classe anônima = MouseAdapter já implementa todos os eventos de mouse vazios; sobrescreve-se só o que interessa
                public void mouseMoved(MouseEvent e) { // o mouse se moveu
                    int i = itemEm(e.getPoint()); // descobre sobre qual opção ele está
                    if (i >= 0) sel = i; // se está sobre alguma, ela passa a ser a selecionada
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); // mão sobre uma opção, seta normal fora
                }
                public void mousePressed(MouseEvent e) { // o botão do mouse foi pressionado
                    int i = itemEm(e.getPoint()); // descobre a opção
                    if (i >= 0) escolher(i); // e a executa
                }
            }; // fim da classe anônima
            addMouseListener(m); addMouseMotionListener(m); // registra o mesmo objeto para cliques e para movimentos
        }

        Rectangle botao(int i) { return new Rectangle(310, 330 + i * 92, 380, 68); } // método que retorna o retângulo (em coordenadas lógicas) da opção i

        void calcularEscala() { // método que calcula a escala e o deslocamento do desenho
            escala = Math.min(getWidth() / (double) W, getHeight() / (double) H); // menor proporção entre largura e altura, para caber sem distorcer
            if (escala <= 0) escala = 1; // proteção: janela ainda sem tamanho
            dx = (getWidth() - W * escala) / 2; // sobra horizontal dividida por 2
            dy = (getHeight() - H * escala) / 2; // sobra vertical dividida por 2
        }

        int itemEm(Point p) { // método que descobre qual opção está no ponto p (ou -1)
            calcularEscala(); // atualiza a escala
            Point l = new Point((int) ((p.x - dx) / escala), (int) ((p.y - dy) / escala)); // converte o ponto da janela para coordenadas lógicas
            for (int i = 0; i < itens.length; i++) if (botao(i).contains(l)) return i; // procura a opção cujo retângulo contém o ponto
            return -1; // nenhuma
        }

        void mover(int d) { sel = (sel + d + itens.length) % itens.length; } // método que move a seleção de forma circular (o resto da divisão faz "dar a volta")

        void escolher(int i) { // método que executa a opção i
            sel = i; // marca como selecionada
            if (i == 0) novoJogo(); // opção 0: novo jogo
            else if (i == 1) abrirRanking(); // opção 1: ranking
            else System.exit(0); // opção 2: encerra o programa
        }

        /** O cabo da opcao escolhida desliza para dentro da porta; os outros se soltam. */
        void animar() { // método que avança a animação dos cabos
            for (int i = 0; i < plug.length; i++) // percorre cada opção
                plug[i] = Math.max(0, Math.min(1, plug[i] + (i == sel ? 0.12f : -0.12f))); // o cabo da selecionada avança, os outros recuam; Math.max/min mantêm o valor entre 0 e 1
        }

        void texto(Graphics2D g, String s, int x, int y, Color c, Font f, boolean centro) { // método que escreve um texto (versão com coordenadas inteiras)
            g.setFont(f); // define a fonte
            g.setColor(c); // define a cor
            if (centro) x -= g.getFontMetrics().stringWidth(s) / 2; // recua metade da largura para centralizar
            g.drawString(s, x, y); // desenha
        }

        @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho completo do menu
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            pintarFundo(g, getWidth(), getHeight(), animMenu); // fundo animado
            calcularEscala(); // calcula a escala
            g.translate(dx, dy); // desloca para centralizar
            g.scale(escala, escala); // aplica a escala
            double esquerda = -dx / escala - 30;   // borda esquerda da janela, em coordenadas logicas

            // logo: a internet (globo) ligada por cabos a 4 computadores, com pacotes indo e voltando
            int gx = 500, gy = 122, gr = 54; // centro (gx, gy) e raio do globo
            int[][] pc = {{318, 78}, {318, 168}, {682, 78}, {682, 168}}; // matriz = posição (x, y) de cada um dos 4 computadores
            int[] par = {1, 5, 3, 7};   // laranja, verde, azul, marrom
            for (int k = 0; k < pc.length; k++) { // percorre cada computador
                double x1 = pc[k][0], y1 = pc[k][1]; // posição dele
                pintarCabo(g, new Line2D.Double(x1, y1, gx, gy), CORES[par[k]], 5, false); // cabo do computador até o globo
                g.setColor(Color.WHITE); // branco
                for (int n = 0; n < 2; n++) { // 2 pacotes de dados por cabo
                    double t = (animMenu * 0.7 + n * 0.5 + k * 0.23) % 1.0; // posição do pacote ao longo do cabo (0 a 1); o resto da divisão por 1 faz voltar ao início
                    if (k % 2 == 1) t = 1 - t;   // metade envia, metade recebe
                    g.fill(new Ellipse2D.Double(x1 + (gx - x1) * t - 3, y1 + (gy - y1) * t - 3, 6, 6)); // bolinha do pacote: interpolação entre as duas pontas do cabo
                }
            }
            for (int k = 0; k < pc.length; k++) { // desenha os 4 computadores
                int x = pc[k][0] - 27, y = pc[k][1] - 20; // canto superior esquerdo do monitor
                g.setColor(BORDA); // cor da base
                g.fillRect(x + 23, y + 36, 8, 7); // haste
                g.fillRoundRect(x + 12, y + 42, 30, 5, 4, 4); // pé
                g.setPaint(new GradientPaint(0, y, new Color(56, 62, 80), 0, y + 36, new Color(38, 43, 56))); // degradê do monitor
                g.fillRoundRect(x, y, 54, 36, 8, 8); // moldura
                g.setColor(new Color(16, 26, 22)); // verde bem escuro
                g.fillRoundRect(x + 5, y + 5, 44, 26, 4, 4); // tela
                g.setColor(Math.sin(animMenu * 8 + k * 2) > -0.3 ? new Color(0, 230, 100) : new Color(40, 70, 55)); // LED que pisca conforme o seno do tempo
                g.fillOval(x + 9, y + 9, 7, 7); // círculo do LED
                g.setColor(new Color(120, 255, 170, 150)); // verde claro translúcido
                g.fillRect(x + 20, y + 11, 24, 2); g.fillRect(x + 9, y + 20, 30, 2); g.fillRect(x + 9, y + 25, 20, 2); // três linhas imitando texto na tela
                g.setColor(BORDA); // cor do contorno
                g.drawRoundRect(x, y, 54, 36, 8, 8); // contorno do monitor
            }
            g.setColor(new Color(80, 200, 255, 40)); // azul translúcido
            g.fillOval(gx - gr - 8, gy - gr - 8, 2 * gr + 16, 2 * gr + 16); // halo em volta do globo
            g.setPaint(new GradientPaint(0, gy - gr, new Color(26, 78, 150), 0, gy + gr, new Color(14, 30, 62))); // degradê azul
            g.fillOval(gx - gr, gy - gr, 2 * gr, 2 * gr); // disco do globo
            g.setStroke(new BasicStroke(1.6f)); // espessura do traço
            g.setColor(new Color(80, 200, 255, 170)); // azul claro
            g.drawLine(gx - gr, gy, gx + gr, gy); // linha do equador
            g.drawLine(gx, gy - gr, gx, gy + gr); // meridiano central
            g.drawOval(gx - 26, gy - gr, 52, 2 * gr); // meridianos laterais (uma elipse)
            g.drawLine(gx - 47, gy - 27, gx + 47, gy - 27); // paralelo de cima
            g.drawLine(gx - 47, gy + 27, gx + 47, gy + 27); // paralelo de baixo
            g.setStroke(new BasicStroke(3)); // traço grosso
            g.setColor(DESTAQUE); // cor de destaque
            g.drawOval(gx - gr, gy - gr, 2 * gr, 2 * gr); // contorno do globo
            g.setStroke(new BasicStroke(1)); // volta ao traço fino

            Font base = getFont(); // fonte padrão do painel
            texto(g, "MONTE O CABO DE REDE", 500, 262, Color.WHITE, base.deriveFont(Font.BOLD, 46f), true); // título do jogo
            texto(g, "RJ45 T568A  ·  RJ45 T568B  ·  Keystone Cat5e", 500, 292, DESTAQUE, // subtítulo com as três fases...
                    new Font(Font.MONOSPACED, Font.BOLD, 15), true); // ...em fonte monoespaçada

            for (int i = 0; i < itens.length; i++) { // percorre cada opção do menu
                Rectangle b = botao(i); // retângulo da opção
                double cy = b.getCenterY(); // centro vertical
                float h = plug[i] * plug[i] * (3 - 2 * plug[i]);   // suaviza o movimento
                // cabo vindo da borda esquerda ate o plugue
                double frente = b.x - 34 + 44 * h, fim = frente - 58; // posição da frente do plugue (entra na porta conforme h cresce) e do fim dele
                double sy = cy + (i - 1) * 120; // altura em que o cabo sai da borda esquerda
                double[] bz = {esquerda, sy, esquerda + (fim - esquerda) * 0.55, sy, fim - (fim - esquerda) * 0.45, cy, fim, cy}; // 8 números = os 4 pontos de controle de uma curva de Bézier cúbica
                pintarCabo(g, new CubicCurve2D.Double(bz[0], bz[1], bz[2], bz[3], bz[4], bz[5], bz[6], bz[7]), cor[i], 10, false); // desenha o cabo curvo
                if (h > 0.05f) { // se o cabo está (quase) conectado
                    // pacotes de dados correndo pelo cabo conectado
                    g.setColor(new Color(255, 255, 255, (int) (220 * h))); // branco com opacidade proporcional a h
                    for (int k = 0; k < 4; k++) { // 4 pacotes
                        double t = (animMenu * 0.9 + k / 4.0) % 1.0, u = 1 - t; // parâmetro t da curva (0 a 1) e seu complemento u
                        double x = u * u * u * bz[0] + 3 * u * u * t * bz[2] + 3 * u * t * t * bz[4] + t * t * t * bz[6]; // fórmula da Bézier cúbica para a coordenada x
                        double y = u * u * u * bz[1] + 3 * u * u * t * bz[3] + 3 * u * t * t * bz[5] + t * t * t * bz[7]; // a mesma fórmula para y
                        g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6)); // bolinha do pacote
                    }
                }
                pintarPlugue(g, frente, cy, 1, cor[i]); // plugue na ponta do cabo

                // a porta (botao)
                if (h > 0) { // se o cabo começou a entrar
                    g.setColor(new Color(cor[i].getRed(), cor[i].getGreen(), cor[i].getBlue(), (int) (45 * h))); // cor da opção, translúcida
                    g.fillRoundRect(b.x - 6, b.y - 6, b.width + 12, b.height + 12, 26, 26); // halo em volta da porta
                }
                g.setPaint(new GradientPaint(0, b.y, new Color(56, 62, 80), 0, b.y + b.height, new Color(38, 43, 56))); // degradê do corpo
                g.fillRoundRect(b.x, b.y, b.width, b.height, 18, 18); // corpo da porta
                g.setColor(new Color(16, 18, 24)); // quase preto
                g.fillRoundRect(b.x + 5, (int) cy - 17, 9, 34, 4, 4);   // entrada do plugue
                g.setStroke(new BasicStroke(1.2f + 1.3f * h)); // a borda engrossa com a conexão
                g.setColor(h > 0.5f ? cor[i] : new Color(96, 104, 124)); // e ganha a cor da opção depois da metade
                g.drawRoundRect(b.x, b.y, b.width, b.height, 18, 18); // contorno
                g.setStroke(new BasicStroke(1)); // volta ao traço fino
                // LED de link
                boolean pisca = h >= 1 && Math.sin(animMenu * 9) > -0.6; // o LED pisca só com o cabo totalmente conectado
                if (pisca) { // quando aceso
                    g.setColor(new Color(0, 230, 100, 60)); // verde translúcido
                    g.fillOval(b.x + 26, (int) cy - 10, 20, 20); // halo do LED
                }
                g.setColor(pisca ? new Color(0, 230, 100) : new Color(78, 84, 98)); // verde ou apagado
                g.fillOval(b.x + 30, (int) cy - 6, 12, 12); // LED
                texto(g, itens[i], b.x + 60, (int) cy + 2, Color.WHITE, base.deriveFont(Font.BOLD, 23f), false); // nome da opção
                texto(g, descr[i], b.x + 60, (int) cy + 21, new Color(165, 174, 194), base.deriveFont(12.5f), false); // descrição
                texto(g, "›", b.x + b.width - 30, (int) cy + 10, h > 0.5f ? cor[i] : new Color(96, 104, 124), // seta à direita...
                        base.deriveFont(Font.BOLD, 30f), false); // ...em fonte grande
            }

            texto(g, "↑ ↓ para escolher   ·   Enter para conectar", 500, 640, new Color(140, 150, 172), base.deriveFont(13f), true); // instruções no rodapé
            g.dispose(); // libera a cópia do contexto gráfico
        }
    } // fim da classe TelaMenu

    /** Tela "Novo jogo": pede o nome do jogador antes de comecar. */
    class TelaNome extends JPanel { // classe interna + herança = a tela de nome É um JPanel
        static final String PEDE_NOME = "Digite seu nome para entrar no ranking:"; // constante com o texto do pedido de nome
        final JTextField campo = new JTextField(16); // campo de texto com largura para 16 caracteres
        final JPasswordField senha = new JPasswordField(16);   // so aparece para o nome protegido; nao mostra o que e digitado; herança na biblioteca = JPasswordField é subclasse de JTextField
        final JLabel sub = new JLabel(PEDE_NOME); // subtítulo (muda quando pede a senha)
        final JLabel erro = new JLabel(" "); // mensagem de erro (um espaço mantém a altura reservada)
        boolean pedindoSenha; // true enquanto a tela espera a senha

        TelaNome() { // construtor: monta a tela
            setLayout(new GridBagLayout()); // GridBagLayout sem restrições deixa o conteúdo no centro
            JPanel caixa = new JPanel() { // classe anônima = caixa central com fundo arredondado
                @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho da caixa
                    Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
                    suavizar(g); // antisserrilhamento
                    g.setColor(new Color(30, 34, 46, 240)); // fundo escuro quase opaco
                    g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28); // retângulo arredondado preenchido
                    g.setStroke(new BasicStroke(1.5f)); // espessura do traço
                    g.setColor(new Color(96, 104, 124)); // cor da borda
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28); // contorno
                    g.dispose(); // libera a cópia
                }
            }; // fim da classe anônima
            caixa.setOpaque(false); // sem fundo padrão (aparece só o desenho acima)
            caixa.setLayout(new BoxLayout(caixa, BoxLayout.Y_AXIS)); // componentes empilhados na vertical
            caixa.setBorder(BorderFactory.createEmptyBorder(30, 48, 28, 48)); // margens internas

            JLabel titulo = new JLabel("NOVO JOGO"); // rótulo do título
            titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 30f)); // fonte grande em negrito
            titulo.setForeground(Color.WHITE); // branco
            sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 14f)); // subtítulo em fonte normal
            sub.setForeground(new Color(165, 174, 194)); // cinza claro
            erro.setFont(erro.getFont().deriveFont(Font.BOLD, 13f)); // erro em negrito
            erro.setForeground(new Color(255, 190, 70)); // alaranjado

            for (JTextField c : new JTextField[]{campo, senha}) { // polimorfismo = o mesmo laço configura o campo de nome e o de senha, pois os dois SÃO JTextField
                c.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22)); // fonte monoespaçada grande
                c.setHorizontalAlignment(JTextField.CENTER); // texto centralizado
                c.setBackground(new Color(18, 21, 30)); // fundo bem escuro
                c.setForeground(Color.WHITE); // letra branca
                c.setCaretColor(DESTAQUE); // cor do cursor
                c.setSelectionColor(AZUL_CABO); // cor do fundo da seleção
                c.setSelectedTextColor(Color.WHITE); // cor do texto selecionado
                c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DESTAQUE, 2, true), // borda composta: linha azul de 2 pixels...
                        BorderFactory.createEmptyBorder(8, 12, 8, 12))); // ...mais margem interna
                c.setMaximumSize(campo.getPreferredSize()); // não deixa o campo esticar além do tamanho preferido
                c.addActionListener(e -> confirmar()); // Enter dentro do campo chama confirmar()
            }
            senha.setVisible(false); // o campo de senha começa escondido

            BotaoCabo voltar = new BotaoCabo("Voltar", new Color(150, 160, 180)); // botão "Voltar"
            BotaoCabo conectar = new BotaoCabo("Conectar e jogar", CORES[1]); // botão "Conectar e jogar"
            voltar.setFocusable(false); conectar.setFocusable(false);   // o foco fica sempre no campo do nome
            voltar.addActionListener(e -> mostrar(MENU)); // "Voltar" mostra o menu
            conectar.addActionListener(e -> confirmar()); // "Conectar e jogar" confirma o nome
            JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0)); // painel dos dois botões
            botoes.setOpaque(false); // transparente
            botoes.add(voltar); botoes.add(conectar); // adiciona os botões

            for (JComponent c : new JComponent[]{titulo, sub, campo, senha, erro, botoes}) c.setAlignmentX(CENTER_ALIGNMENT); // centraliza horizontalmente todos os componentes da caixa
            caixa.add(titulo); // adiciona o título
            caixa.add(Box.createVerticalStrut(8)); // espaço vertical
            caixa.add(sub); // subtítulo
            caixa.add(Box.createVerticalStrut(18)); // espaço
            caixa.add(campo); // campo do nome
            caixa.add(senha); // campo da senha (no mesmo lugar; só um aparece por vez)
            caixa.add(Box.createVerticalStrut(6)); // espaço
            caixa.add(erro); // mensagem de erro
            caixa.add(Box.createVerticalStrut(12)); // espaço
            caixa.add(botoes); // botões
            add(caixa); // coloca a caixa na tela
        }

        void abrir() { // método que prepara a tela cada vez que ela é aberta
            pedirSenha(false); // volta ao modo "pedir nome"
            campo.setText(jogador == null || admin() ? "" : jogador); // preenche com o nome do jogador anterior (menos na primeira vez ou se era o adm)
            campo.selectAll(); // seleciona o texto, para digitar por cima
            erro.setText(" "); // limpa a mensagem de erro
        }

        /** Alterna entre o campo do nome e o campo da senha (que esconde o nome digitado). */
        void pedirSenha(boolean sim) { // método que alterna entre pedir o nome (false) e pedir a senha (true)
            pedindoSenha = sim; // guarda o modo
            senha.setText(""); // limpa o campo de senha
            campo.setVisible(!sim); // o campo de nome aparece quando NÃO está pedindo senha
            senha.setVisible(sim); // o campo de senha aparece quando está
            sub.setText(sim ? "Este nome é protegido. Digite a senha:" : PEDE_NOME); // troca o subtítulo
            revalidate(); // recalcula o layout
            (sim ? senha : campo).requestFocusInWindow(); // o ternário escolhe qual campo recebe o foco
        }

        void confirmar() { // método que confirma o que foi digitado
            if (pedindoSenha) { // se estava pedindo a senha
                boolean certa = senhaAdm(senha.getPassword()); // getPassword retorna char[]; senhaAdm confere e apaga
                senha.setText(""); // limpa o campo
                if (certa) entrarNoJogo(NOME_ADMIN, true); // senha certa: entra como adm, com o nome público
                else erro.setText("Senha incorreta."); // senha errada: mostra o erro :P
                return; // termina o método aqui
            }
            String s = limparNome(campo.getText()); // nome digitado, já limpo
            if (s.isEmpty()) { erro.setText("O nome não pode ficar vazio."); return; } // validação = nome vazio não é aceito
            if (s.equalsIgnoreCase(NOME_ADMIN)) { erro.setText("Este nome é reservado. Escolha outro."); return; } // o nome público do adm é reservado
            if (loginAdm(s)) { // se o nome é o login do adm
                campo.setText("");   // o login do adm some da tela assim que e reconhecido
                erro.setText(" "); // limpa a mensagem
                pedirSenha(true); // passa a pedir a senha
                return; // e espera a próxima confirmação
            }
            entrarNoJogo(s, false); // jogador comum: entra direto
        }

        @Override protected void paintComponent(Graphics g0) { // sobrescrita = fundo da tela de nome
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            pintarFundo(g, getWidth(), getHeight(), animMenu); // fundo animado dos menus
            g.dispose(); // libera a cópia
        }
    } // fim da classe TelaNome

    /** Tela do ranking (top 10 partidas), com botao para voltar. */
    class TelaRanking extends JPanel { // classe interna + herança = a tela de ranking É um JPanel
        final JPanel centro = new JPanel(new BorderLayout()); // área central, onde entra a tabela
        static final String SUB_TOP10 = "Top 10 partidas  ·  mais pontos primeiro, desempate pelo menor tempo"; // subtítulo normal
        static final String SUB_ADM = "Modo adm  ·  todas as partidas  ·  clique em uma linha para poder excluir"; // subtítulo do modo adm
        final BotaoCabo voltar = new BotaoCabo("", CORES[3]); // botão de voltar (o texto é definido em atualizar())
        final BotaoCabo excluir = new BotaoCabo("Excluir do ranking (adm)", VERMELHO); // botão de excluir, só do adm
        final JLabel sub = new JLabel(SUB_TOP10, SwingConstants.CENTER); // rótulo do subtítulo, centralizado
        final JLabel aviso = new JLabel(" ", SwingConstants.CENTER); // rótulo de avisos
        JTable tabelaAtual; // tabela que está na tela (null se não houver)
        List<Resultado> listados = new ArrayList<>(); // resultados listados, na mesma ordem das linhas da tabela

        TelaRanking() { // construtor: monta a tela
            setLayout(new BorderLayout(0, 18)); // BorderLayout com 18 pixels de espaço vertical
            setBorder(BorderFactory.createEmptyBorder(130, 90, 110, 90)); // margens internas
            JLabel titulo = new JLabel("RANKING", SwingConstants.CENTER); // título centralizado
            titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 38f)); // fonte grande em negrito
            titulo.setForeground(Color.WHITE); // branco
            sub.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); // subtítulo em fonte monoespaçada
            sub.setForeground(DESTAQUE); // cor de destaque
            JPanel norte = new JPanel(new GridLayout(2, 1, 0, 4)); // painel do topo: grade de 2 linhas e 1 coluna
            norte.setOpaque(false); // transparente
            norte.add(titulo); norte.add(sub); // título e subtítulo
            centro.setOpaque(false); // centro transparente
            voltar.setFocusable(false); // o botão não recebe foco
            voltar.addActionListener(e -> fecharRanking()); // ao clicar, fecha o ranking
            excluir.setFocusable(false); // o botão não recebe foco
            excluir.addActionListener(e -> excluirSelecionado()); // ao clicar, exclui a linha selecionada
            aviso.setFont(aviso.getFont().deriveFont(Font.BOLD, 13f)); // aviso em negrito
            aviso.setForeground(AMARELO); // amarelo
            JPanel linha = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0)); // linha dos botões
            linha.setOpaque(false); // transparente
            linha.add(voltar); linha.add(excluir); // adiciona os dois botões
            JPanel sul = new JPanel(new BorderLayout(0, 4)); // painel de baixo
            sul.setOpaque(false); // transparente
            sul.add(linha, BorderLayout.CENTER); // botões no centro
            sul.add(aviso, BorderLayout.SOUTH); // aviso embaixo
            add(norte, BorderLayout.NORTH); // topo
            add(centro, BorderLayout.CENTER); // centro
            add(sul, BorderLayout.SOUTH); // base
        }

        /** Recarrega o arquivo do ranking e remonta a tabela. */
        void atualizar() { // método que recarrega os dados cada vez que a tela é aberta
            voltar.setText(origemRanking.equals(JOGO) ? "Voltar ao jogo (Esc)" // o texto do botão de voltar depende de onde o ranking foi aberto...
                    : origemRanking.equals(RESULTADO) ? "Voltar ao resultado (Esc)" : "Voltar ao menu inicial (Esc)"); // ...(ternários encadeados)
            centro.removeAll(); // remove a tabela antiga
            tabelaAtual = null; // ainda não há tabela nova
            List<Resultado> todos = carregarRanking(); // lê o arquivo
            // o adm ve todas as partidas (para poder excluir qualquer uma); os outros, o top 10
            listados = admin() ? todos : todos.subList(0, Math.min(10, todos.size())); // subList retorna um pedaço da lista; Math.min evita pedir mais itens do que existem
            sub.setText(admin() ? SUB_ADM : SUB_TOP10); // subtítulo conforme o perfil
            excluir.setVisible(admin() && !todos.isEmpty()); // "Excluir" só aparece para o adm e se houver partidas
            aviso.setText(" "); // limpa o aviso
            if (todos.isEmpty()) { // ranking vazio:
                JLabel vazio = new JLabel("Ninguém terminou uma partida ainda. Seja o primeiro!", SwingConstants.CENTER); // mensagem centralizada
                vazio.setFont(vazio.getFont().deriveFont(Font.PLAIN, 17f)); // fonte
                vazio.setForeground(new Color(200, 206, 220)); // cor
                centro.add(vazio); // coloca a mensagem no centro
            } else { // ranking com partidas:
                centro.add(tabela(listados)); // monta a tabela e a coloca no centro
            }
            centro.revalidate(); // recalcula o layout
        }

        /** So o adm chega aqui: exclui a partida da linha selecionada, ou todas as partidas daquele jogador. */
        void excluirSelecionado() { // método que exclui partidas do ranking (só o adm chega aqui)
            if (!admin()) return; // segurança = confere de novo se é o adm, mesmo com o botão escondido para os outros
            int i = tabelaAtual == null ? -1 : tabelaAtual.getSelectedRow(); // linha selecionada (ou -1)
            if (i < 0) { aviso.setText("Clique em uma linha da tabela para escolher quem excluir."); return; } // sem seleção: avisa e sai
            Resultado r = listados.get(i); // resultado correspondente à linha
            long total = carregarRanking().stream().filter(x -> x.nome().equalsIgnoreCase(r.nome())).count(); // stream + lambda = conta quantas partidas esse jogador tem
            String nome = semHtml(r.nome()); // nome protegido para uso em HTML
            JLabel texto = new JLabel("<html><div style='width:350px'>Jogador <b>" + nome + "</b>: partida de <b>" + r.pontos() // texto do diálogo em HTML (o JLabel interpreta textos que começam com <html>)...
                    + " pontos</b> em " + r.data() + " (" + (i + 1) + "º lugar).<br>" // ...pontos, data e posição...
                    + "<font color='" + hex(new Color(165, 174, 194)) + "'>Esse jogador tem " + total // ...parte em cinza...
                    + (total == 1 ? " partida" : " partidas") + " no ranking. Uma cópia do arquivo fica em " // ...singular ou plural...
                    + ARQ_RANKING.getFileName() + ".bak antes de apagar.</font></div></html>"); // ...nome do arquivo de cópia
            texto.setFont(texto.getFont().deriveFont(Font.PLAIN, 14f)); // fonte do texto
            int op = perguntar("EXCLUIR DO RANKING", VERMELHO, texto, // abre o diálogo de confirmação e recebe a opção escolhida
                    new String[]{"Cancelar", "Excluir só esta partida", "Excluir tudo de " + r.nome()}, // as três opções
                    new String[]{"não apaga nada", "tira do ranking apenas a linha selecionada", // a descrição de cada uma...
                            "tira do ranking " + (total == 1 ? "a única partida" : "as " + total + " partidas") + " desse jogador"}, // ...(a terceira muda conforme a quantidade)
                    new Color[]{CORES[5], CORES[1], VERMELHO}); // a cor de cada uma
            if (op < 1) return; // Esc (-1) ou "Cancelar" (0): não faz nada
            int n = op == 1 ? removerDoRanking(r::equals, true) // referência de método = r::equals como Predicate: o equals gerado pelo record compara todos os campos
                            : removerDoRanking(x -> x.nome().equalsIgnoreCase(r.nome()), false); // lambda = como Predicate: todas as partidas com o mesmo nome
            atualizar(); // recarrega a tabela
            aviso.setText(n < 0 ? "Não consegui regravar o arquivo do ranking." : n + (n == 1 ? " partida excluída." : " partidas excluídas.")); // mostra o resultado da operação
        }

        JScrollPane tabela(List<Resultado> todos) { // método que monta a tabela do ranking dentro de uma área com rolagem
            String[] colunas = {"#", "Jogador", "Pontos", "Tempo", "Testes", "Fases", "Dicas", "Padrão", "Data"}; // títulos das colunas
            DefaultTableModel m = new DefaultTableModel(colunas, 0) { // classe anônima = modelo de dados da tabela, começando com 0 linhas
                public boolean isCellEditable(int r, int c) { return false; } // sobrescrita = nenhuma célula pode ser editada
            }; // fim da classe anônima
            for (int i = 0; i < todos.size(); i++) { // percorre cada resultado
                Resultado r = todos.get(i); // resultado i
                m.addRow(new Object[]{i + 1, r.nome(), r.pontos(), formatarTempo(r.tempo()), r.testes(), // autoboxing = acrescenta a linha; os int viram Integer ao entrar no vetor de Object...
                        r.fasesTexto(), r.dicas() ? "sim" : "não", r.padrao(), r.data()}); // ...junto com os textos das demais colunas
            }
            Color fundo = new Color(36, 41, 54), claro = new Color(222, 228, 240); // cores de fundo e de texto
            Color[] podio = {new Color(255, 210, 60), new Color(200, 208, 222), new Color(222, 150, 90)}; // ouro, prata e bronze
            JTable t = new JTable(m); // cria a tabela a partir do modelo
            t.setFont(t.getFont().deriveFont(14f)); // fonte tamanho 14
            t.setRowHeight(32); // altura das linhas
            t.setBackground(fundo); // fundo
            t.setGridColor(new Color(58, 64, 82)); // cor das linhas de grade
            t.setShowVerticalLines(false); // sem linhas verticais
            t.setRowSelectionAllowed(admin());   // so o adm seleciona linhas (para excluir)
            t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // no máximo uma linha selecionada
            tabelaAtual = t; // guarda a tabela para o botão de excluir
            t.setFocusable(false); // a tabela não recebe foco
            t.setFillsViewportHeight(true); // a tabela ocupa toda a altura disponível
            t.getColumnModel().getColumn(0).setMaxWidth(44); // coluna "#" estreita
            t.getColumnModel().getColumn(1).setPreferredWidth(190); // coluna "Jogador" larga
            t.getTableHeader().setReorderingAllowed(false); // não deixa arrastar as colunas
            DefaultTableCellRenderer cab = new DefaultTableCellRenderer(); // renderizador do cabeçalho
            cab.setHorizontalAlignment(SwingConstants.CENTER); // centralizado
            cab.setBackground(new Color(20, 24, 34)); // fundo escuro
            cab.setForeground(DESTAQUE); // texto na cor de destaque
            cab.setFont(t.getFont().deriveFont(Font.BOLD)); // negrito
            cab.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4)); // margem interna
            t.getTableHeader().setDefaultRenderer((tb, v, s, f, r, c) -> { cab.setText(String.valueOf(v)); return cab; }); // lambda de 6 parâmetros = implementa a interface TableCellRenderer: define o texto e retorna o componente
            t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() { // classe anônima = renderizador das células comuns
                @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) { // sobrescrita = chamado pela tabela para desenhar cada célula
                    super.getTableCellRendererComponent(tb, v, false, false, r, c); // super.método() = aproveita o comportamento padrão
                    setHorizontalAlignment(c == 1 ? LEFT : CENTER); // nome à esquerda, o resto centralizado
                    setBackground(tb.isRowSelected(r) ? new Color(24, 74, 150) : r % 2 == 0 ? fundo : new Color(42, 48, 63)); // azul se selecionada; senão, alterna duas cores por linha par/ímpar
                    setForeground(r < 3 && c <= 2 ? podio[r] : claro);   // ouro, prata e bronze
                    setFont(tb.getFont().deriveFont(r < 3 && c <= 2 ? Font.BOLD : Font.PLAIN)); // negrito para o pódio
                    return this; // this = retorna o próprio renderizador, que é um componente
                }
            }); // fim da classe anônima e da chamada
            JScrollPane sp = new JScrollPane(t); // área com barra de rolagem contendo a tabela
            sp.getViewport().setBackground(fundo); // fundo da área visível
            sp.setBackground(new Color(20, 24, 34)); // fundo da moldura
            sp.setBorder(BorderFactory.createLineBorder(new Color(96, 104, 124))); // borda
            return sp; // retorna o componente pronto
        }

        @Override protected void paintComponent(Graphics g0) { // sobrescrita = fundo da tela de ranking
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            suavizar(g); // antisserrilhamento
            pintarFundo(g, getWidth(), getHeight(), animMenu); // fundo animado dos menus
            g.dispose(); // libera a cópia
        }
    } // fim da classe TelaRanking

    static class Fio { int id; int pino = -1; Rectangle casa; } // classe aninhada estática: um fio: identificador (0..7), pino onde está (-1 = bandeja) e posição dele na bandeja

    /** Resultado de uma fase dentro da partida. */
    static class Fase { boolean ok, desistiu; int seg, falhas, pontos, estrelas; } // valores padrão = atributos boolean começam false e int começam 0

    class Board extends JPanel { // classe interna + herança = o tabuleiro É um JPanel e guarda todo o estado da partida
        int fase = 0;              // 0 = RJ45 T568A, 1 = RJ45 T568B, 2 = Keystone
        int[] padrao = PADRAO_FASE[0]; // padrão de cores exigido na fase atual
        Fio[] fios = new Fio[8]; // os 8 fios, indexados pelo id
        Fio[] slot = new Fio[8]; // slot[p] = fio que está no pino p (null = pino vazio)
        Fio arrastando; // fio que está sendo arrastado (null = nenhum)
        int origem = -1;           // pino de onde o fio arrastado saiu (-1 = bandeja)
        int offX, offY;            // onde o mouse "segurou" o fio
        int alvo = -1;             // pino destacado durante o arraste
        Point mouse = new Point(); // posição atual do mouse durante o arraste
        int ledPasso = -1;         // -1 = sem teste; 0..8 = quantos LEDs ja acenderam
        boolean[] ok = new boolean[8]; // resultado do último teste, pino a pino
        int velocidade = 0;        // 0 = sem link, 100 ou 1000 Mbps
        float anim = 0; // contador de tempo das animações
        boolean conectando;        // plugue deslizando para dentro do testador
        float plugado = 0;         // 0 = plugue solto, 1 = encaixado no testador
        Point ponteiro = new Point(-1, -1); // última posição do ponteiro (para o efeito de passar por cima)
        final javax.swing.Timer teste = new javax.swing.Timer(300, e -> passoTeste()); // timer = a cada 300 ms acende mais um LED do teste
        final javax.swing.Timer proximaFase = new javax.swing.Timer(1800, e -> iniciarFase(fase + 1)); // timer = espera 1,8 s e inicia a fase seguinte

        // estado da partida (3 fases)
        Fase[] fases = novasFases(); // resultado de cada fase da partida
        int testesTotal;           // soma de todos os testes da partida (passa de uma fase para a outra)
        int testesFase;            // testes feitos na fase atual (inclui os de cada "refazer")
        long faseInicio;           // 0 = cronometro da fase ainda nao comecou
        int segAnteriores;         // tempo das fases ja encerradas
        boolean usouDicas, faseEncerrada, partidaEncerrada; // situação da partida

        // dicas ganhas com testes errados (acumulam durante a partida)
        int dicasDisp; // dicas disponíveis para usar
        int dicasUsadas;           // quantas dicas foram gastas na partida (aparece no resultado)
        boolean modoAdm;           // o adm jogou com "Mostrar dicas" ligado
        boolean[] revelado = new boolean[8];   // pinos revelados por dica na fase atual

        // escala para a janela redimensionavel
        double escala = 1, dx = 0, dy = 0; // escala e deslocamento do desenho

        Board() { // construtor do tabuleiro
            setPreferredSize(new Dimension(LARGURA, ALTURA)); // tamanho preferido
            setBackground(new Color(30, 34, 44)); // cor de fundo
            proximaFase.setRepeats(false); // este timer dispara uma vez só a cada restart()
            new javax.swing.Timer(30, e -> { // timer da animação: roda a cada 30 ms
                anim += 0.02f; // avança o tempo da animação
                atualizarTempo(); // atualiza o cronômetro
                if (conectando) { // se o plugue está indo para o testador
                    plugado = Math.min(1, plugado + 0.09f); // avança o encaixe, limitado a 1
                    if (plugado >= 1) { conectando = false; comecarTeste(); } // encaixou: começa o teste
                }
                if (velocidade > 0 || arrastando != null || todosEncaixados()) repaint(); // só redesenha se houver algo se movendo na tela
            }).start(); // liga o timer
            MouseAdapter m = new MouseAdapter() { // classe anônima = tratador dos eventos de mouse do tabuleiro
                public void mousePressed(MouseEvent e) { pegar(logico(e.getPoint())); } // botão pressionado: tenta pegar um fio
                public void mouseDragged(MouseEvent e) { arrastar(logico(e.getPoint())); } // mouse arrastado: move o fio
                public void mouseReleased(MouseEvent e) { soltar(logico(e.getPoint())); } // botão solto: larga o fio
                public void mouseMoved(MouseEvent e) { // mouse movido sem botão
                    Point p = ponteiro = logico(e.getPoint()); // guarda a posição, em coordenadas lógicas
                    boolean clicavel = fioEm(p) != null || (mostrarBotaoTestar() && BOTAO_TESTAR.contains(p)); // há algo clicável sob o ponteiro?
                    setCursor(Cursor.getPredefinedCursor(clicavel ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); // mão se houver, seta normal se não
                }
            }; // fim da classe anônima
            addMouseListener(m); addMouseMotionListener(m); // registra o tratador para cliques e para movimentos
        }

        Fase[] novasFases() { // método que cria um vetor de fases zeradas
            Fase[] f = new Fase[N_FASES]; // vetor de objetos = o new cria só as posições, ainda com null
            for (int i = 0; i < N_FASES; i++) f[i] = new Fase(); // então cria um objeto Fase para cada posição
            return f; // retorna o vetor
        }

        boolean keystone() { return fase == FASE_KEYSTONE; } // método que checa se a fase atual é a da keystone

        void calcularEscala() { // método que calcula a escala do desenho
            escala = Math.min(getWidth() / (double) LARGURA, getHeight() / (double) ALTURA); // menor proporção, para caber sem distorcer
            if (escala <= 0) escala = 1; // proteção: janela ainda sem tamanho
            dx = (getWidth() - LARGURA * escala) / 2; // sobra horizontal dividida por 2
            dy = (getHeight() - ALTURA * escala) / 2; // sobra vertical dividida por 2
        }

        Point logico(Point p) { // método que converte um ponto da janela para coordenadas lógicas
            calcularEscala(); // atualiza a escala
            return new Point((int) ((p.x - dx) / escala), (int) ((p.y - dy) / escala)); // desfaz o deslocamento e a escala
        }

        // fluxo da partida 
        void novaPartida() { // método que começa uma partida do zero
            proximaFase.stop(); // cancela a troca de fase pendente
            fases = novasFases(); // fases zeradas
            testesTotal = 0; segAnteriores = 0; dicasDisp = 0; dicasUsadas = 0; // zera os contadores (atribuições separadas por ";")
            partidaEncerrada = false; // a partida está aberta
            usouDicas = modoAdm = admin() && dicasBox.isSelected(); // atribuição encadeada = os dois ficam true só se o adm estiver com as dicas ligadas
            iniciarFase(0); // vai para a fase 1 (índice 0)
        }

        void iniciarFase(int f) { // método que prepara a fase f
            fase = f; // guarda o número da fase
            padrao = PADRAO_FASE[f]; // padrão de cores dela
            testesFase = 0; faseInicio = 0; faseEncerrada = false; // zera os contadores da fase
            Arrays.fill(revelado, false); // nenhum pino revelado
            embaralharFios(); // fios de volta à bandeja, em ordem aleatória
            status.setText("Fase " + (fase + 1) + ": monte o " + FASES[fase] + " no padrão " + nomePadrao() // mensagem inicial da fase...
                    + ". Errou um teste? Ganha 1 dica (tecla D)."); // ...com o lembrete das dicas
        }

        /** Devolve os fios para a bandeja em ordem aleatoria (nao zera testes, tempo nem dicas). */
        void embaralharFios() { // método que embaralha os fios
            if (partidaEncerrada) { status.setText("Partida terminada. Aperte N para jogar de novo."); return; } // partida terminada: só avisa
            if (faseEncerrada) return;   // trocando de fase
            resetTeste(); // cancela qualquer teste em andamento
            arrastando = null; alvo = -1; // ninguém está sendo arrastado
            List<Integer> ordem = new ArrayList<>(); // List<Integer> = lista com os números de 0 a 7
            for (int i = 0; i < 8; i++) ordem.add(i); // autoboxing = o int i vira Integer ao entrar na lista
            Collections.shuffle(ordem); // embaralha a lista
            for (int k = 0; k < 8; k++) { // percorre cada posição da bandeja
                Fio fio = new Fio(); // instanciação = cria um objeto Fio
                fio.id = ordem.get(k); // id sorteado
                fio.casa = new Rectangle(70 + k * 95, 470, 30, 110); // retângulo do fio na bandeja
                fios[fio.id] = fio; // guarda o fio na posição do seu id
            }
            Arrays.fill(slot, null); // esvazia todos os pinos
            atualizarCabecalho(); // atualiza os textos do topo
            repaint(); // redesenha
        }

        String nomePadrao() { return padrao == T568B ? "T568B" : "T568A"; } // método que retorna o nome do padrão atual, "==" compara se é o MESMO vetor (referência)

        void dicasAlteradas() { // método chamado quando o adm marca ou desmarca "Mostrar dicas"
            if (admin() && dicasBox.isSelected() && !partidaEncerrada) usouDicas = modoAdm = true; // ligar as dicas marca a partida como "usou dicas"
            repaint(); // redesenha
        }

        /** Gasta 1 dica para revelar o fio certo de um pino (de preferencia um que esteja vazio ou errado). */
        void usarDica() { // método que usa uma dica
            if (partidaEncerrada || faseEncerrada || teste.isRunning() || conectando || arrastando != null) return; // cláusula de guarda = não faz nada em momentos em que não se pode usar dica
            if (dicasDisp == 0) { // sem dicas:
                status.setText("💡 Você não tem dicas. Ganha 1 a cada teste errado."); // avisa
                return; // e sai
            }
            int p = -1; // pino a revelar (-1 = ainda não achou)
            for (int i = 0; i < 8 && p < 0; i++) // primeira busca: para quando achar (p >= 0)
                if (!revelado[i] && (slot[i] == null || slot[i].id != padrao[i])) p = i; // um pino não revelado que esteja vazio ou com o fio errado
            for (int i = 0; i < 8 && p < 0; i++) // segunda busca, se a primeira não achou:
                if (!revelado[i]) p = i; // qualquer pino ainda não revelado
            if (p < 0) { status.setText("Todos os pinos desta fase já foram revelados."); return; } // nada para revelar
            revelado[p] = true; // marca o pino como revelado
            dicasDisp--; // gasta uma dica
            dicasUsadas++; // conta a dica usada
            usouDicas = true; // a partida perde o bônus "sem dicas"
            status.setText("💡 Dica: o pino " + (p + 1) + " leva o fio " + NOMES[padrao[p]] + ".  (dicas restantes: " + dicasDisp + ")"); // mostra a dica no rodapé
            atualizarCabecalho(); // atualiza o topo
            repaint(); // redesenha
        }

        void resetTeste() { // método que cancela o teste e solta o plugue
            teste.stop(); ledPasso = -1; velocidade = 0; Arrays.fill(ok, false); // para o timer, apaga os LEDs e zera a velocidade
            conectando = false; plugado = 0; // plugue solto
        }

        int segFase() { // método que retorna os segundos da fase atual
            if (faseInicio == 0) return 0; // cronômetro ainda não começou
            return (int) ((System.currentTimeMillis() - faseInicio) / 1000); // diferença de milissegundos convertida em segundos ([CAST] de long para int)
        }

        int segTotal() { return segAnteriores + (faseEncerrada ? 0 : segFase()); } // método que retorna o tempo total: fases anteriores + fase atual (se ainda estiver aberta)

        /** Fecha o cronometro da fase atual e guarda o tempo dela. */
        int encerrarFase() { // método que fecha o cronômetro da fase
            int s = segFase(); // tempo desta fase
            segAnteriores += s; // soma ao total
            faseEncerrada = true; // marca a fase como encerrada
            return s; // retorna o tempo dela
        }

        void atualizarCabecalho() { // método que atualiza os textos da barra do topo
            faseLbl.setText("Fase " + (fase + 1) + "/" + N_FASES + ": " + FASES[fase]); // fase atual
            jogadorLbl.setText("👤 " + jogador + (admin() ? " (adm)" : "")); // nome do jogador
            dicaBtn.setText("💡 Usar dica (D): " + dicasDisp); // quantidade de dicas no botão
            dicaBtn.setEnabled(dicasDisp > 0); // botão habilitado só com dica disponível
            StringBuilder sb = new StringBuilder("<html>"); // texto em HTML
            for (int i = 0; i < N_FASES; i++) { // percorre cada fase
                if (i > 0) sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"); // espaços entre uma fase e outra
                sb.append(FASES[i]).append(' ').append(resumo(fases[i], fase > i || partidaEncerrada)); // nome da fase e suas estrelas
            }
            progresso.setText(sb.toString()); // coloca o texto no rótulo
            atualizarTempo(); // atualiza também o tempo
        }

        String resumo(Fase f, boolean passou) { // método que monta o texto colorido com a situação de uma fase
            // estrelas coloridas: 1 = vermelha, 2 = amarela, 3 = verde
            if (f.ok) return "<font color='" + hex(corEstrelas(f.estrelas)) + "'>" + estrelas(f.estrelas) + "</font>"; // fase concluída: estrelas na cor certa
            return "<font color='" + hex(CINZA) + "'>" + (passou ? (f.desistiu ? "(desistiu)" : "(pulou)") : estrelas(0)) + "</font>"; // não concluída: "(desistiu)", "(pulou)" ou estrelas vazias, em cinza
        }

        void atualizarTempo() { // método que atualiza o rótulo de tempo
            String t = "⏱ " + formatarTempo(segTotal()) + "   Testes: " + testesTotal + (usouDicas ? "   Dicas usadas: " + dicasUsadas + (modoAdm ? " + adm" : "") : "   (sem dicas = pontos x2)"); // monta o texto: tempo, testes e situação das dicas
            if (!t.equals(tempoLbl.getText())) tempoLbl.setText(t); // só altera o rótulo se o texto mudou (evita redesenhar à toa)
        }

        // "Conectar e testar o cabo" (plugue + testador, aparece na bandeja)
        final Rectangle BOTAO_TESTAR = new Rectangle(150, 472, 570, 148); // área clicável do "conectar e testar"

        boolean todosEncaixados() { // método que checa se todos os pinos têm fio
            for (Fio s : slot) if (s == null) return false; // achou um pino vazio = não
            return true; // nenhum vazio = sim
        }

        boolean mostrarBotaoTestar() { // método que checa se o "botão" de testar deve aparecer
            return todosEncaixados() && arrastando == null && !teste.isRunning() && !faseEncerrada && !partidaEncerrada // 8 fios encaixados, nada sendo arrastado, sem teste rodando, fase e partida abertas...
                    && !conectando && plugado == 0; // ...e plugue ainda solto
        }

        /** Ja mexeu em algum fio ou passou de fase nesta partida? */
        boolean emAndamento() { return !partidaEncerrada && (faseInicio != 0 || testesTotal > 0 || fase > 0); } // método que checa se a partida está aberta e já com alguma atividade

        /** Refazer: os fios no pino certo ficam onde estao, os errados voltam para a bandeja. */
        int devolverErrados() { // método que manda os fios errados de volta p/ a bandeja
            resetTeste(); // cancela o teste
            int certos = 0; // contador de fios certos
            for (int p = 0; p < 8; p++) { // percorre cada pino
                Fio f = slot[p]; // fio que está no pino
                if (f == null) continue; // pino vazio: passa para o próximo
                if (f.id == padrao[p]) certos++; // fio certo: fica e é contado
                else { slot[p] = null; f.pino = -1; } // fio errado: esvazia o pino e manda o fio para a bandeja
            }
            repaint(); // redesenha
            return certos; // retorna quantos ficaram
        }

        // geometria
        Rectangle slotRect(int pino) { // método que retorna o retângulo do pino na tela
            if (!keystone()) return new Rectangle(250 + pino * 60, 110, 40, 250); // plugue RJ45: 8 pinos lado a lado, de 60 em 60 pixels
            for (int r = 0; r < 4; r++) { // keystone: procura o pino nas 4 linhas
                if (KS_ESQ[r] == pino) return new Rectangle(250, 105 + r * 66, 190, 46); // está na coluna da esquerda
                if (KS_DIR[r] == pino) return new Rectangle(520, 105 + r * 66, 190, 46); // está na coluna da direita
            }
            return new Rectangle(); // não achou: retângulo vazio (não deve acontecer)
        }

        Rectangle encaixado(int pino) { // método que retorna o retângulo do fio quando está encaixado no pino
            Rectangle s = slotRect(pino); // retângulo do pino
            return keystone() ? new Rectangle(s.x + 8, s.y + 8, s.width - 16, s.height - 16) // na keystone o fio fica deitado, com 8 pixels de margem...
                              : new Rectangle(s.x + 5, s.y + 25, s.width - 10, s.height - 25); // ...e no plugue fica em pé, abaixo do contato dourado
        }

        Rectangle limites(Fio f) { // método que retorna o retângulo atual de um fio, onde quer que ele esteja
            if (f == arrastando) return new Rectangle(mouse.x - offX, mouse.y - offY, 30, 110); // sendo arrastado: acompanha o mouse
            return f.pino >= 0 ? encaixado(f.pino) : f.casa; // encaixado: posição do pino; solto: posição na bandeja
        }

        Fio fioEm(Point p) { // método que descobre qual fio está no ponto p
            for (int i = 7; i >= 0; i--) if (fios[i] != null && limites(fios[i]).contains(p)) return fios[i]; // percorre do último para o primeiro e retorna o primeiro fio que contém o ponto
            return null; // nenhum
        }

        int slotMaisProximo(Point p) { // método que acha o pino mais próximo do ponto p (ou -1 se todos estiverem a mais de 70 pixels)
            int melhor = -1; double menor = 70; // melhor candidato e menor distância até agora
            for (int i = 0; i < 8; i++) { // percorre cada pino
                Rectangle s = slotRect(i); // retângulo dele
                double d = s.contains(p) ? 0 : Point.distance(p.x, p.y, s.getCenterX(), s.getCenterY()); // distância 0 se o ponto está dentro; senão, distância até o centro
                if (d < menor) { menor = d; melhor = i; } // achou um mais perto: guarda
            }
            return melhor; // retorna o melhor
        }

        // mouse
        void pegar(Point p) { // método chamado quando o botão do mouse é pressionado no ponto p
            if (mostrarBotaoTestar() && BOTAO_TESTAR.contains(p)) { iniciarTeste(); return; } // clicou no "conectar e testar": inicia o teste e sai
            if (teste.isRunning() || conectando || faseEncerrada) return;   // espera o testador / a troca de fase
            Fio f = fioEm(p); // fio sob o ponteiro
            if (f == null) return; // nenhum: sai
            if (faseInicio == 0) faseInicio = System.currentTimeMillis(); // o cronômetro da fase começa na primeira vez que se mexe em um fio
            resetTeste(); // mexer nos fios cancela o teste anterior
            if (f.pino >= 0) { // o fio estava encaixado em um pino:
                offX = 15; offY = 55;        // fio encaixado tem outro formato: segura pelo meio
                origem = f.pino; slot[f.pino] = null; f.pino = -1; // lembra de onde saiu, esvazia o pino e marca o fio como solto
            } else { // o fio estava na bandeja:
                offX = p.x - f.casa.x; offY = p.y - f.casa.y; // guarda em que ponto do fio o mouse o segurou
                origem = -1; // origem = bandeja
            }
            arrastando = f; mouse = p; alvo = slotMaisProximo(p); // passa a arrastar o fio e já calcula o pino mais próximo
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); // cursor de "mover"
            repaint(); // redesenha
        }

        void arrastar(Point p) { // método chamado quando o mouse é arrastado
            if (arrastando == null) return; // sem fio na mão: ignora
            mouse = p; alvo = slotMaisProximo(p); // atualiza a posição e o pino de destino
            repaint(); // redesenha
        }

        void soltar(Point p) { // método chamado quando o botão do mouse é solto
            if (arrastando == null) return; // sem fio na mão: ignora
            Fio f = arrastando; arrastando = null; alvo = -1; // guarda o fio em f e encerra o arraste
            int melhor = slotMaisProximo(p); // pino mais próximo de onde soltou
            if (melhor >= 0) { // se há um pino perto
                Fio antigo = slot[melhor]; // fio que já estava nesse pino
                if (antigo != null) { // se havia um
                    // troca: o fio que estava ali vai para o pino de onde o outro saiu (ou volta para a bandeja)
                    antigo.pino = origem; // ele assume a origem do fio arrastado
                    if (origem >= 0) slot[origem] = antigo; // se a origem era um pino, passa a ocupá-lo
                }
                slot[melhor] = f; f.pino = melhor; // encaixa o fio arrastado no pino
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cursor volta a ser a mão
            int n = 0; for (Fio s : slot) if (s != null) n++; // conta os pinos ocupados
            status.setText(n == 8 ? "Tudo encaixado! Clique em CONECTAR E TESTAR O CABO (ou aperte T)." : "Fios encaixados: " + n + "/8"); // mensagem: tudo encaixado ou a contagem
            repaint(); // redesenha
        }

        // teste 
        void iniciarTeste() { // método que atende o pedido de teste (clique, T ou Enter)
            if (partidaEncerrada) { status.setText("Partida terminada. Aperte N para jogar de novo."); return; } // partida terminada: só avisa
            if (arrastando != null || teste.isRunning() || conectando || faseEncerrada) return; // momentos em que não se pode testar
            for (Fio s : slot) if (s == null) { status.setText("Encaixe os 8 fios antes de testar."); return; } // se faltar algum fio, avisa e sai
            resetTeste(); // limpa o teste anterior
            conectando = true;   // o plugue desliza ate o testador; ao encaixar chama comecarTeste()
            status.setText("Conectando o cabo ao testador..."); // mensagem
        }

        void comecarTeste() { // método que começa a testar (chamado quando o plugue termina de encaixar)
            for (int i = 0; i < 8; i++) ok[i] = slot[i].id == padrao[i]; // compara cada pino com o padrão; o resultado da comparação (true/false) vai para o vetor
            testesTotal++; testesFase++; // conta o teste na partida e na fase
            velocidade = 0; ledPasso = 0; teste.restart(); // zera os LEDs e (re)inicia o timer do teste
            status.setText("Testando pino a pino..."); // mensagem
            atualizarTempo(); // atualiza o contador de testes na tela
        }

        void passoTeste() { // método que faz um passo do teste (a cada 300 ms)
            ledPasso++; // acende mais um LED
            if (ledPasso >= 8) { teste.stop(); finalizar(); } // acendeu os 8: para o timer e calcula o resultado
            repaint(); // redesenha
        }

        void finalizar() { // método que calcula o resultado do teste
            StringBuilder erros = new StringBuilder(); // montador da lista de pinos errados
            for (int i = 0; i < 8; i++) if (!ok[i]) erros.append(i + 1).append(' '); // acrescenta o número de cada pino errado
            String msg, titulo, explicacao; // declaração múltipla = três Strings, que recebem valor nos ramos abaixo
            if (erros.length() == 0) { // nenhum erro:
                velocidade = 1000; // link Gigabit :D
                status.setText("✔ Link Gigabit (1000 Mbps)! Os 4 pares estão corretos. A internet passou!"); // mensagem de sucesso
                faseConcluida(); // encerra a fase com sucesso
                return; // e sai do método
            } else if (ok[0] && ok[1] && ok[2] && ok[5]) { // pinos 1, 2, 3 e 6 certos (índices 0, 1, 2 e 5):
                velocidade = 100; // link de 100 Mbps
                titulo = "LINK SÓ A 100 MBPS"; // título do diálogo
                explicacao = "Os pinos 1, 2, 3 e 6 estão certos, mas o Gigabit usa os 8 fios."; // explicação
                msg = "⚠ Link só a 100 Mbps: pinos 1,2,3,6 OK, mas erros nos pinos: " + erros + "(Gigabit usa os 8 fios)."; // mensagem do rodapé
            } else { // qualquer outro caso:
                titulo = "SEM LINK"; // sem link :(
                explicacao = "Lembre: 1-2 e 3-6 são os pares de dados do Fast Ethernet."; // explicação
                msg = "✘ Sem link. Pinos errados: " + erros + "→ Lembre: 1-2 e 3-6 são os pares de dados do Fast Ethernet."; // mensagem do rodapé
            }
            dicasDisp++;   // cada teste errado da direito a 1 dica
            atualizarCabecalho(); // atualiza o topo
            status.setText(msg + "  💡 +1 dica (" + dicasDisp + ")"); // mostra a mensagem e a dica ganha
            repaint(); // redesenha
            perguntarAposErro(titulo, explicacao, erros.toString().trim()); // abre o diálogo de erro
        }

        /**
         * Errou o teste = mostra os pinos certos/errados e deixa escolher
         * RJ45: refazer (conta mais testes) ou seguir para a proxima fase.
         * Keystone: refazer ou desistir (salva o que fez no ranking).
         */
        void perguntarAposErro(String titulo, String explicacao, String erros) { // método responsável pelo diálogo depois de um teste errado
            boolean ks = keystone(); // é a fase da keystone?
            String cinza = hex(new Color(165, 174, 194)); // cor cinza em formato HTML
            JLabel texto = new JLabel("<html><div style='width:350px'>" // texto do diálogo em HTML...
                    + "O cabo <b>" + FASES[fase] + "</b> não passou. Pinos errados: <b><font color='" + hex(VERMELHO) + "'>" + erros + "</font></b><br>" // ...nome do cabo e pinos errados em vermelho...
                    + "<font color='" + cinza + "'>" + explicacao + "</font><br><br>" // ...explicação em cinza...
                    + "<b><font color='" + hex(AMARELO) + "'>+1 dica!</font></b> Você tem <b>" + dicasDisp + "</b> " // ...dica ganha...
                    + (dicasDisp == 1 ? "dica" : "dicas") + " para usar (tecla D ou o botão).<br>" // ...singular ou plural...
                    + "Testes até agora: <b>" + testesTotal + "</b>" // ...total de testes...
                    + (ks ? "" : " <font color='" + cinza + "'>(continua contando na próxima fase)</font>") + "</div></html>"); // ...e um lembrete, se não for a keystone
            texto.setFont(texto.getFont().deriveFont(Font.PLAIN, 14f)); // fonte
            texto.setAlignmentX(LEFT_ALIGNMENT); // alinhado à esquerda
            JComponent pinos = new PinosTeste(ok.clone()); // polimorfismo = variável JComponent com um PinosTeste; ok.clone() entrega uma cópia do vetor
            pinos.setAlignmentX(LEFT_ALIGNMENT); // alinhado à esquerda
            JPanel corpo = new JPanel(); // painel que junta os pinos e o texto
            corpo.setOpaque(false); // transparente
            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS)); // empilhados na vertical
            corpo.add(pinos); // fileira de pinos
            corpo.add(Box.createVerticalStrut(14)); // espaço
            corpo.add(texto); // texto

            String[] opcoes = {(ks ? "Refazer a " : "Refazer o ") + FASES[fase], ks ? "Desistir" : "Ir para a próxima fase: " + FASES[fase + 1]}; // títulos das duas opções (mudam na keystone)
            String[] descr = {"os fios certos ficam no lugar, os errados voltam para a bandeja", // descrição de cada opção...
                    ks ? "salva no ranking o que você fez até aqui (" + pontosPartida() + " pontos)" : "pula o " + FASES[fase] + " com 0 pontos nele"}; // ...(a segunda muda na keystone)
            int r = perguntar(titulo, velocidade == 100 ? AMARELO : VERMELHO, corpo, opcoes, descr, // abre o diálogo: amarelo se o link ficou em 100 Mbps, vermelho se não houve link...
                    new Color[]{CORES[5], ks ? VERMELHO : CORES[1]}); // ...com a cor de cada opção
            if (r != 1) { refazer(); return; }   // Esc tambem conta como refazer

            Fase f = fases[fase]; // registro da fase atual
            f.ok = false; f.desistiu = ks; f.falhas = testesFase; f.seg = encerrarFase(); f.pontos = 0; f.estrelas = 0; // marca a fase como não concluída e sem pontos
            if (ks) { terminarPartida(true); return; } // na keystone, a segunda opção é desistir: termina a partida
            String pulada = FASES[fase]; // nome da fase pulada
            iniciarFase(fase + 1); // vai para a próxima fase
            status.setText("Você pulou o " + pulada + " (0 pontos nele). Fase " + (fase + 1) + ": monte o " // mensagem explicando o que aconteceu...
                    + FASES[fase] + " no padrão " + nomePadrao() + "."); // ...e o que fazer agora
        }

        void refazer() { // método que refaz a fase
            int certos = devolverErrados(); // manda os errados de volta e conta os certos
            status.setText("Refazendo a fase " + (fase + 1) + " (" + FASES[fase] + "): " + certos // mensagem...
                    + " fio(s) certo(s) ficaram no lugar, os errados voltaram para a bandeja. (próximo teste: nº " + (testesTotal + 1) + ")"); // ...com o número do próximo teste
        }

        /** Soma dos pontos das fases concluidas (x2 se nao usou dicas). */
        int pontosPartida() { // método que calcula a pontuação da partida
            int pontos = 0; // acumulador
            for (Fase f : fases) pontos += f.pontos; // soma os pontos de cada fase
            return usouDicas ? pontos : pontos * 2; // dobra se não usou dicas
        }

        void faseConcluida() { // método chamado quando a fase é concluída com sucesso :D
            Fase f = fases[fase]; // registro da fase atual
            f.ok = true; // marca como concluída
            f.seg = encerrarFase(); // guarda o tempo
            f.falhas = testesFase - 1; // erros = testes feitos menos o que deu certo
            f.pontos = Math.max(100, 1000 - 5 * f.seg - 150 * f.falhas); // regra de pontuação = 1000 - 5 por segundo - 150 por erro, com mínimo de 100
            f.estrelas = (f.falhas == 0 && f.seg <= 60) ? 3 : (f.falhas <= 1 && f.seg <= 120) ? 2 : 1; // regra das estrelas: 3 = sem erro em até 60 s, 2 = até 1 erro em até 120 s, senão 1
            atualizarCabecalho(); // atualiza o topo
            repaint(); // redesenha

            if (fase < N_FASES - 1) { // se ainda há fases
                // acertou: vai sozinho para a proxima fase depois de mostrar o link funcionando
                status.setText("✔ " + FASES[fase] + " perfeito! " + estrelas(f.estrelas) + " " + f.pontos // mensagem com as estrelas e os pontos...
                        + " pontos. Indo para a fase " + (fase + 2) + ": " + FASES[fase + 1] + "..."); // ...e a próxima fase
                proximaFase.restart(); // dispara o timer que troca de fase
            } else { // era a última fase:
                terminarPartida(false); // termina a partida (sem desistência)
            }
        }

        void terminarPartida(boolean desistiu) { // método que encerra a partida
            partidaEncerrada = true; // marca como encerrada
            int pontos = pontosPartida(); // pontuação final
            StringBuilder codigo = new StringBuilder(); // código das fases para o arquivo
            for (Fase f : fases) codigo.append(f.ok ? '1' : '0'); // '1' para fase concluída, '0' para não concluída
            int seg = segAnteriores; // tempo total

            int recordeAnterior = 0; // recorde anterior do jogador
            for (Resultado r : carregarRanking()) // a lista vem ordenada, então a primeira partida dele é a melhor
                if (r.nome().equalsIgnoreCase(jogador)) { recordeAnterior = r.pontos(); break; } // achou: guarda os pontos e interrompe o laço (break)
            salvarResultado(new Resultado(jogador, pontos, seg, testesTotal, codigo.toString(), usouDicas, // instanciação de record = cria o Resultado e grava no arquivo...
                    "A/B/KS-B", LocalDate.now().toString())); // ...com a data de hoje
            atualizarCabecalho(); // atualiza o topo

            // o resultado ja foi salvo no ranking acima; a tela de resultado cuida de recomecar o jogo
            telaResultado.abrir(fases, desistiu, seg, testesTotal, dicasUsadas, modoAdm, usouDicas, pontos, recordeAnterior); // entrega os dados à tela de resultado
            mostrar(RESULTADO); // e a mostra
        }

        // desenho 
        void desenharFio(Graphics2D g, int id, Rectangle r, float alpha) { // método que desenha um fio dentro do retângulo r, alpha = opacidade
            Composite oldC = g.getComposite(); // guarda a composição atual
            Shape oldClip = g.getClip(); // guarda a área de recorte atual
            Stroke oldS = g.getStroke(); // guarda o traço atual
            if (alpha < 1f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); // se for translúcido, aplica a opacidade
            Shape forma = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 10, 10); // polimorfismo = retângulo arredondado guardado como Shape
            g.setColor(LISTRADO[id] ? Color.WHITE : CORES[id]); // branco se listrado; senão, a cor do fio
            g.fill(forma); // preenche
            if (LISTRADO[id]) { // fio listrado:
                g.clip(forma); // limita o desenho ao interior do fio
                g.setColor(CORES[id]); // cor da listra
                g.setStroke(new BasicStroke(8)); // listras grossas
                for (int d = -r.height; d < r.width; d += 22) // linhas diagonais a cada 22 pixels...
                    g.drawLine(r.x + d, r.y + r.height, r.x + d + r.height, r.y); // ...de baixo para cima
                g.setClip(oldClip); // restaura o recorte
            }
            g.setStroke(new BasicStroke(1.5f)); // traço fino
            g.setColor(new Color(0, 0, 0, 150)); // preto translúcido
            g.draw(forma); // contorno
            g.setStroke(oldS); // restaura o traço
            g.setComposite(oldC); // restaura a composição
        }

        /** Painel no estilo das portas do menu: gradiente escuro com borda fina. */
        void painel(Graphics2D g, int x, int y, int w, int h) { // método que desenha um painel arredondado
            g.setPaint(new GradientPaint(0, y, new Color(52, 58, 75), 0, y + h, new Color(38, 43, 56))); // degradê
            g.fillRoundRect(x, y, w, h, 20, 20); // fundo
            g.setColor(BORDA); // cor da borda
            g.drawRoundRect(x, y, w, h, 20, 20); // contorno
        }

        void texto(Graphics2D g, String s, int x, int y, Color c, float tam, boolean centro) { // método que escreve um texto com o tamanho de fonte dado
            g.setFont(getFont().deriveFont(tam)); // fonte do painel no tamanho pedido
            g.setColor(c); // cor
            if (centro) x -= g.getFontMetrics().stringWidth(s) / 2; // centraliza, se pedido
            g.drawString(s, x, y); // desenha
        }

        @Override protected void paintComponent(Graphics g0) { // sobrescrita = desenho completo do tabuleiro, o Swing chama este método sempre que precisa repintar
            Graphics2D g = (Graphics2D) g0.create(); // cópia do contexto gráfico
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // antisserrilhamento das formas
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // e das letras
            // fundo igual ao do menu: gradiente escuro pontilhado
            g.setPaint(new GradientPaint(0, 0, new Color(22, 26, 37), 0, getHeight(), new Color(34, 39, 52))); // degradê escuro
            g.fillRect(0, 0, getWidth(), getHeight()); // pinta o fundo
            g.setColor(new Color(255, 255, 255, 12)); // branco quase transparente
            for (int y = 14; y < getHeight(); y += 28) // linhas do pontilhado...
                for (int x = 14; x < getWidth(); x += 28) g.fillRect(x, y, 2, 2); // ...e colunas
            calcularEscala(); // calcula a escala
            g.translate(dx, dy); // centraliza
            g.scale(escala, escala); // aplica a escala
            boolean dicasAdm = admin() && dicasBox.isSelected(); // o adm está com "Mostrar dicas" ligado?
            boolean keystone = keystone(); // é a fase da keystone?
            Color amarelo = new Color(255, 210, 60); // amarelo das dicas

            // corpo do conector
            if (keystone) { // keystone:
                g.setColor(new Color(240, 240, 245)); // plástico claro
                g.fillRoundRect(220, 70, 520, 310, 24, 24); // corpo
                g.setColor(new Color(150, 150, 160)); g.drawRoundRect(220, 70, 520, 310, 24, 24); // contorno cinza
                texto(g, "KEYSTONE CAT.5e  (punch-down)", 480, 92, new Color(90, 90, 100), 13f, true); // título
                texto(g, "Padrão " + nomePadrao() + " (etiqueta da tomada)", 480, 372, new Color(90, 90, 100), 12f, true); // legenda com o padrão
            } else { // plugue RJ45:
                g.setColor(new Color(190, 220, 240, 110)); // plástico transparente azulado
                g.fillRoundRect(220, 70, 520, 310, 24, 24); // corpo
                g.setColor(new Color(170, 175, 185)); g.setStroke(new BasicStroke(3)); // contorno mais grosso
                g.drawRoundRect(220, 70, 520, 310, 24, 24); g.setStroke(new BasicStroke(1)); // desenha e volta ao traço fino
                texto(g, "PLUGUE RJ45 " + nomePadrao() + " (vista de baixo, pino 1 à esquerda)", 480, 395, new Color(190, 195, 205), 12f, true); // legenda
            }

            // pinos
            for (int p = 0; p < 8; p++) { // percorre cada pino
                Rectangle s = slotRect(p); // retângulo dele
                if (!keystone) { // no plugue:
                    g.setColor(new Color(235, 190, 40)); // dourado
                    g.fillRoundRect(s.x + 14, 76, 12, 28, 4, 4); // contato metálico
                    texto(g, String.valueOf(p + 1), s.x + 20, 64, revelado[p] ? amarelo : Color.WHITE, 15f, true); // número do pino (amarelo se foi revelado)
                }
                g.setColor(keystone ? new Color(0, 0, 0, 25) : new Color(255, 255, 255, 40)); // fundo do pino
                g.fillRoundRect(s.x, s.y, s.width, s.height, 10, 10); // preenche
                g.setColor(new Color(120, 125, 135)); // cinza
                g.drawRoundRect(s.x, s.y, s.width, s.height, 10, 10); // contorno
                if (keystone) texto(g, "Pino " + (p + 1), s.x + 8, s.y + 18, new Color(70, 70, 80), 13f, false); // na keystone, o número fica dentro do pino

                // fio "fantasma": dica revelada (qualquer jogador) ou "Mostrar dicas" (so adm)
                if (slot[p] == null && (dicasAdm || revelado[p])) { // pino vazio e com dica disponível
                    int esperado = padrao[p]; // fio esperado nesse pino
                    desenharFio(g, esperado, encaixado(p), 0.28f); // desenha o fio com 28% de opacidade
                    if (keystone) { // keystone:
                        texto(g, NOMES[esperado], s.x + s.width - 8 - g.getFontMetrics().stringWidth(NOMES[esperado]), s.y + 18, new Color(60, 60, 70), 12f, false); // nome do fio alinhado à direita
                    } else { // plugue:
                        textoVertical(g, s, NOMES[esperado]); // nome escrito na vertical
                    }
                }
                // contorno amarelo nos pinos revelados por dica
                if (revelado[p]) { // se o pino foi revelado
                    g.setColor(amarelo); // amarelo
                    g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 4}, 0)); // traço tracejado
                    g.drawRoundRect(s.x - 5, s.y - 5, s.width + 10, s.height + 10, 14, 14); // contorno em volta do pino
                    g.setStroke(new BasicStroke(1)); // volta ao traço fino
                }
            }

            // destaque do pino onde o fio vai cair
            if (arrastando != null && alvo >= 0) { // se há um fio na mão e um pino de destino
                Rectangle s = slotRect(alvo); // retângulo do destino
                g.setColor(new Color(80, 200, 255, 60)); // azul translúcido
                g.fillRoundRect(s.x, s.y, s.width, s.height, 10, 10); // preenche
                g.setColor(new Color(80, 200, 255)); // azul
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 6}, anim * 40)); // tracejado animado (o deslocamento do tracejado muda com o tempo)
                g.drawRoundRect(s.x - 3, s.y - 3, s.width + 6, s.height + 6, 12, 12); // contorno
                g.setStroke(new BasicStroke(1)); // volta ao traço fino
            }

            // fios encaixados
            for (Fio f : fios) if (f.pino >= 0 && f != arrastando) { // percorre cada fio encaixado (menos o que está na mão)
                desenharFio(g, f.id, encaixado(f.pino), 1f); // desenha o fio no pino
                if (ledPasso >= 0 && f.pino < ledPasso) { // se o teste já passou por este pino
                    Rectangle s = slotRect(f.pino); // retângulo do pino
                    g.setColor(ok[f.pino] ? new Color(0, 220, 90) : new Color(240, 50, 50)); // verde se certo, vermelho se errado
                    g.setStroke(new BasicStroke(3)); // traço grosso
                    g.drawRoundRect(s.x - 2, s.y - 2, s.width + 4, s.height + 4, 12, 12); // contorno colorido
                    g.setStroke(new BasicStroke(1)); // volta ao traço fino
                }
            }

            // bandeja
            painel(g, 20, 440, 830, 190); // painel da bandeja
            texto(g, todosEncaixados() ? "Todos os fios encaixados! Agora conecte o cabo no testador:" // texto da bandeja: muda quando todos estão encaixados...
                    : "Fios soltos: arraste para o conector (soltar em cima de outro fio troca os dois)", 36, 460, new Color(170, 180, 200), 13f, false); // ...posição, cor e tamanho
            for (Fio f : fios) if (f.pino < 0 && f != arrastando) { // percorre cada fio solto (menos o que está na mão)
                desenharFio(g, f.id, f.casa, 1f); // desenha o fio
                texto(g, NOMES[f.id], f.casa.x + 15, f.casa.y + f.casa.height + 16, new Color(200, 205, 215), 10.5f, true); // e o nome embaixo
            }
            if (arrastando != null) { // se há um fio na mão
                // sombra + fio na mao
                Rectangle r = limites(arrastando); // posição dele
                g.setColor(new Color(0, 0, 0, 90)); // preto translúcido
                g.fillRoundRect(r.x + 6, r.y + 8, r.width, r.height, 10, 10); // sombra deslocada
                desenharFio(g, arrastando.id, r, 1f); // o fio, por cima de tudo
            }

            if (todosEncaixados() && arrastando == null && !partidaEncerrada) desenharConexao(g); // com tudo encaixado, desenha o plugue e o testador

            painelTestador(g, amarelo); // painel do testador, à direita
            painelAtalhos(g); // painel dos atalhos
            g.dispose(); // libera a cópia do contexto gráfico
        }

        /** Em vez de um botao: o plugue do cabo pronto para entrar no testador. Clicar (ou T) conecta e testa. */
        void desenharConexao(Graphics2D g) { // método que desenha o plugue, o cabo e o testador dentro da bandeja
            Rectangle b = BOTAO_TESTAR; // área do "botão"
            boolean clicavel = mostrarBotaoTestar(); // pode ser clicado agora?
            boolean hover = clicavel && b.contains(ponteiro); // o mouse está em cima?
            Shape oldClip = g.getClip(); // guarda o recorte atual

            // placa de fundo
            g.setColor(new Color(30, 34, 44)); // cor escura
            g.fillRoundRect(b.x, b.y, b.width, b.height, 22, 22); // fundo
            g.setStroke(new BasicStroke(hover ? 2.5f : 1.3f)); // borda mais grossa com o mouse em cima
            g.setColor(hover ? DESTAQUE : new Color(84, 92, 112)); // cor da borda
            g.drawRoundRect(b.x, b.y, b.width, b.height, 22, 22); // contorno
            g.setStroke(new BasicStroke(1)); // volta ao traço fino
            g.clip(new RoundRectangle2D.Float(b.x + 2, b.y + 2, b.width - 4, b.height - 4, 20, 20)); // limita o desenho ao interior da placa

            // plugue: balanca em direcao a porta enquanto espera, desliza para dentro ao conectar
            int tx = b.x + b.width - 200, ty = b.y + 12, tw = 184, th = 88;   // testador
            double cy = ty + th / 2.0; // centro vertical do testador
            float e = plugado * plugado * (3 - 2 * plugado); // suavização do movimento de encaixe
            double solto = tx - 62 + (hover ? 14 : 7) * Math.sin(anim * 5); // posição do plugue solto: balança com o seno do tempo (mais com o mouse em cima)
            double frente = solto + (tx + 34 - solto) * e; // interpola entre a posição solta e a encaixada
            double fim = frente - 116; // fim do plugue
            pintarCabo(g, new CubicCurve2D.Double(b.x - 20, cy + 26, b.x + 70, cy + 26, fim - 70, cy, fim, cy), AZUL_CABO, 15, false); // cabo em curva de Bézier, da borda até o plugue
            pintarPlugue(g, frente, cy, 2, AZUL_CABO); // plugue em escala 2

            // setas "encaixe aqui"
            if (clicavel) { // se pode clicar
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // traço arredondado
                for (int k = 0; k < 3; k++) { // 3 setas
                    double a = 0.5 + 0.5 * Math.sin(anim * 7 - k * 1.1); // brilho de cada seta, defasado no tempo
                    g.setColor(new Color(80, 200, 255, (int) (40 + 190 * a))); // azul com opacidade variável
                    int sx = (int) frente + 12 + k * 13; // posição x da seta
                    g.drawPolyline(new int[]{sx, sx + 7, sx}, new int[]{(int) cy - 9, (int) cy, (int) cy + 9}, 3); // desenha o ">" com 3 pontos
                }
                g.setStroke(new BasicStroke(1)); // volta ao traço fino
            }

            // testador (desenhado por cima: a ponta do plugue some dentro da porta)
            g.setPaint(new GradientPaint(0, ty, new Color(84, 91, 110), 0, ty + th, new Color(54, 60, 75))); // degradê do testador
            g.fillRoundRect(tx, ty, tw, th, 14, 14); // corpo
            g.setColor(new Color(120, 128, 148)); // cinza
            g.drawRoundRect(tx, ty, tw, th, 14, 14); // contorno
            g.setColor(new Color(14, 16, 22)); // quase preto
            g.fillRoundRect(tx + 4, (int) cy - 30, 12, 60, 5, 5);   // boca da porta RJ45
            texto(g, "TESTADOR", tx + 30, ty + 21, new Color(200, 206, 220), 11f, false); // rótulo

            boolean testando = conectando || teste.isRunning(); // está conectando ou testando?
            boolean pronto = !testando && plugado >= 1 && ledPasso >= 8; // o teste terminou com o plugue encaixado?
            boolean pisca = Math.sin(anim * 14) > 0; // alterna true/false com o tempo, para piscar
            Color apagado = new Color(40, 44, 54), verde = new Color(0, 230, 100), ambar = new Color(255, 190, 40); // cores dos LEDs
            Color link = apagado, act = apagado; // os dois LEDs começam apagados
            String visor = "aguardando cabo"; // texto do visor
            if (testando) { // durante o teste:
                link = pisca ? ambar : apagado; // LED de link piscando em âmbar
                visor = conectando ? "conectando..." : "testando " + Math.min(8, ledPasso + 1) + "/8"; // visor: "conectando..." ou "testando n/8"
            } else if (pronto) { // teste terminado:
                link = velocidade == 1000 ? verde : velocidade == 100 ? ambar : new Color(240, 50, 50); // verde = 1000, âmbar = 100, vermelho = sem link
                act = velocidade > 0 && pisca ? verde : apagado; // LED de atividade piscando, se houver link
                visor = velocidade > 0 ? "link " + velocidade + " Mbps" : "sem link"; // visor com a velocidade
            }
            g.setColor(link); g.fillOval(tx + tw - 44, ty + 11, 11, 11); // LED de link
            g.setColor(act);  g.fillOval(tx + tw - 26, ty + 11, 11, 11); // LED de atividade
            g.setColor(new Color(16, 26, 22)); // verde bem escuro
            g.fillRoundRect(tx + 28, ty + 34, tw - 42, 36, 8, 8); // fundo do visor
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); // fonte monoespaçada
            g.setColor(new Color(120, 255, 170)); // verde claro
            g.drawString(visor, tx + 28 + (tw - 42 - g.getFontMetrics().stringWidth(visor)) / 2, ty + 57); // texto do visor, centralizado
            g.setClip(oldClip); // restaura o recorte

            // legenda
            int cx = b.x + b.width / 2; // centro horizontal
            String titulo = clicavel ? "CONECTAR E TESTAR O CABO" : testando ? "Testando o cabo..." : "Cabo conectado ao testador"; // legenda conforme a situação
            g.setFont(getFont().deriveFont(Font.BOLD, 19f)); // negrito, tamanho 19
            g.setColor(hover ? DESTAQUE : Color.WHITE); // destaque com o mouse em cima
            g.drawString(titulo, cx - g.getFontMetrics().stringWidth(titulo) / 2, b.y + 122); // desenha centralizado
            if (clicavel) texto(g, "clique aqui ou aperte T / Enter", cx, b.y + 140, new Color(170, 180, 200), 12f, true); // instrução, quando pode clicar
        }

        // texto vertical dentro do pino do RJ45
        void textoVertical(Graphics2D g, Rectangle s, String nome) { // método que escreve o nome do fio na vertical
            java.awt.geom.AffineTransform old = g.getTransform(); // guarda a transformação atual
            g.rotate(-Math.PI / 2, s.x + 25, s.y + s.height - 8); // gira o desenho 90 graus em torno do ponto do texto
            texto(g, nome, s.x + 25, s.y + s.height - 8, new Color(30, 30, 30, 200), 11f, false); // escreve
            g.setTransform(old); // restaura a transformação
        }

        void painelAtalhos(Graphics2D g) { // método que desenha o painel com a lista de atalhos
            int x = 865, y = 440; // posição
            painel(g, x, y, 115, 190); // fundo
            texto(g, "ATALHOS", x + 57, y + 22, DESTAQUE, 12f, true); // título
            String[] linhas = {"T / Enter  testar", "D  usar dica", "R  embaralhar", "N  nova partida", "K  ranking", "M  menu inicial"}; // linhas de texto
            for (int i = 0; i < linhas.length; i++) // percorre cada linha
                texto(g, linhas[i], x + 12, y + 48 + i * 24, new Color(190, 195, 210), 11.5f, false); // escreve, 24 pixels abaixo da anterior
        }

        void painelTestador(Graphics2D g, Color amarelo) { // método que desenha o painel do testador de cabo
            int x = 770, y = 70; // posição
            painel(g, x, y, 210, 310); // fundo
            texto(g, "TESTADOR DE CABO", x + 105, y + 24, DESTAQUE, 13f, true); // título
            for (int i = 0; i < 8; i++) { // percorre cada pino
                int ly = y + 50 + i * 24; // altura da linha
                Color c = new Color(80, 85, 95); // LED apagado
                if (ledPasso >= 0 && i < ledPasso) c = ok[i] ? new Color(0, 230, 100) : new Color(240, 50, 50); // se o teste já passou por ele: verde ou vermelho
                g.setColor(c); g.fillOval(x + 14, ly - 11, 14, 14); // desenha o LED
                String n = slot[i] != null ? NOMES[slot[i].id] : "—"; // nome do fio que está no pino, ou um traço
                texto(g, (i + 1) + "  " + n, x + 38, ly, revelado[i] ? amarelo : new Color(210, 215, 225), 12f, false); // número e nome (amarelo se foi revelado)
                // dica revelada: amostra do fio certo no canto direito
                if (revelado[i]) desenharFio(g, padrao[i], new Rectangle(x + 186, ly - 15, 12, 19), 1f); // amostra do fio certo
            }
            // rede
            int ny = y + 270; // altura do desenho da rede
            g.setColor(new Color(70, 75, 90)); g.fillRoundRect(x + 12, ny - 16, 44, 28, 8, 8); // caixa do PC
            g.fillRoundRect(x + 154, ny - 16, 44, 28, 8, 8); // caixa da Web
            texto(g, "PC", x + 34, ny + 3, Color.WHITE, 12f, true); // rótulo "PC"
            texto(g, "Web", x + 176, ny + 3, Color.WHITE, 12f, true); // rótulo "Web"
            g.setColor(velocidade == 1000 ? new Color(0, 230, 100) : velocidade == 100 ? new Color(240, 190, 40) : new Color(110, 110, 120)); // cor do link: verde, âmbar ou cinza
            g.setStroke(new BasicStroke(3)); g.drawLine(x + 56, ny - 2, x + 154, ny - 2); g.setStroke(new BasicStroke(1)); // linha entre o PC e a Web
            if (velocidade > 0) { // se há link
                int pacotes = velocidade == 1000 ? 5 : 2; // mais pacotes no Gigabit
                for (int k = 0; k < pacotes; k++) { // percorre cada pacote
                    float t = (anim * (velocidade == 1000 ? 2f : 1f) + k / (float) pacotes) % 1f; // posição ao longo da linha (mais rápida no Gigabit)
                    g.fillOval(x + 56 + (int) (t * 92), ny - 6, 8, 8); // bolinha do pacote
                }
                texto(g, velocidade + " Mbps", x + 105, ny + 24, Color.WHITE, 12f, true); // velocidade
            } else { // sem link:
                texto(g, "sem link", x + 105, ny + 24, new Color(170, 170, 180), 12f, true); // texto "sem link"
            }
        }
    } // fim da classe Board

    public static void main(String[] args) { // ponto de entrada = a JVM começa a execução por este método
        SwingUtilities.invokeLater(() -> { // lambda = pede ao Swing para criar a interface na thread de eventos (regra do Swing)
            new RJ45Game().setVisible(true);   // abre no menu inicial; instanciação = cria a janela (executa o construtor) e a torna visível
        }); // fim da lambda
    }
} // fim da classe RJ45Game, GG! <3
