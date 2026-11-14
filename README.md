# Monte o cabo de rede: RJ45 / Cat5e

Jogo em Java (Swing) feito para a disciplina de Programação Orientada a Objetos.
O jogador monta cabos de rede arrastando os 8 fios para os pinos corretos e depois conecta o cabo a um testador, que acende os LEDs pino a pino e mostra se o link subiu.

O arquivo `RJ45Game.java` deste repositório é a versão **comentada linha a linha**: cada linha de código tem, no fim dela, um comentário explicando o que ela faz. Os conceitos de POO aparecem pelo nome no começo do comentário (`herança`, `polimorfismo`, `sobrescrita`...), para facilitar a busca.

> **Observação sobre autoria:** o jogo usa o conteúdo visto em aula e também recursos que vão além dele. Tudo o que vai além do conteúdo das aulas foi feito com auxílio do Claude (assistente de IA da Anthropic). A lista completa está na seção [Relação com o conteúdo da disciplina](#relação-com-o-conteúdo-da-disciplina).

## Como executar

É preciso ter o JDK 17 ou mais novo (o código usa `record` e membros estáticos em classes internas, que exigem Java 16+). Foi testado com o OpenJDK 21.

```bash
javac RJ45Game.java
java RJ45Game
```

O jogo não usa bibliotecas externas: só a biblioteca padrão do Java.

## Como jogar

1. No menu inicial, escolha **Novo jogo** e digite seu nome.
2. Arraste cada fio da bandeja para um pino do conector. Soltar um fio em cima de outro troca os dois de lugar.
3. Com os 8 fios encaixados, clique em **Conectar e testar o cabo**.
4. O testador confere pino a pino:
   - os 8 certos: link Gigabit (1000 Mbps) e a fase é concluída;
   - só os pinos 1, 2, 3 e 6 certos: link de 100 Mbps (não conclui a fase);
   - outro caso: sem link.
5. Errou? Você ganha 1 dica e escolhe entre refazer (os fios certos ficam no lugar) ou pular a fase. Na última fase, a segunda opção é desistir.

### As três fases

| Fase | Conector | Padrão | Ordem dos fios (pino 1 ao 8) |
|---|---|---|---|
| 1 | Plugue RJ45 | T568A | branco-verde, verde, branco-laranja, azul, branco-azul, laranja, branco-marrom, marrom |
| 2 | Plugue RJ45 | T568B | branco-laranja, laranja, branco-verde, azul, branco-azul, verde, branco-marrom, marrom |
| 3 | Keystone Cat5e (punch-down) | T568B | mesma ordem da fase 2, com os pinos dispostos como na etiqueta da tomada: 4-5-2-1 de um lado e 6-3-8-7 do outro |

### Pontuação

- Cada fase concluída vale `1000 − 5 × segundos − 150 × erros`, com mínimo de 100 pontos.
- Fase pulada ou abandonada vale 0.
- Quem termina a partida **sem usar dicas** tem o total multiplicado por 2.
- Estrelas por fase: 3 = nenhum erro em até 60 s; 2 = no máximo 1 erro em até 120 s; 1 = nos demais casos.

### Atalhos de teclado

| Tela | Tecla | Ação |
|---|---|---|
| Jogo | `T` ou `Enter` | testar o cabo |
| Jogo | `D` | usar uma dica |
| Jogo | `R` | embaralhar os fios |
| Jogo | `N` | nova partida |
| Jogo | `K` | ranking |
| Jogo | `M` | menu inicial |
| Menu | `↑` `↓` e `Enter` | escolher e confirmar |
| Nome e ranking | `Esc` | voltar |

### Ranking

Cada partida terminada (ou abandonada) é gravada em um arquivo CSV na pasta pessoal do usuário: `~/.rj45game_partidas.csv`. A tela de ranking mostra as 10 melhores, ordenadas por pontos e, no empate, pelo menor tempo.

Existe um perfil de administrador, protegido por senha, que pode ligar a opção "Mostrar dicas" e excluir partidas do ranking. O login e a senha não ficam no código: só os hashes (SHA-256 e PBKDF2).

## Organização do código

Todo o jogo está em um único arquivo, com a classe principal e as classes aninhadas dentro dela:

| Classe | Tipo | Papel |
|---|---|---|
| `RJ45Game` | `extends JFrame` | a janela: guarda as telas, os atalhos, o ranking e os métodos de desenho compartilhados |
| `Board` | classe interna, `extends JPanel` | o tabuleiro: estado da partida, arraste dos fios, teste e desenho |
| `TelaMenu`, `TelaNome`, `TelaRanking`, `TelaResultado` | classes internas, `extends JPanel` | as outras telas do jogo |
| `BotaoCabo`, `OpcaoPorta` | classes aninhadas estáticas, `extends JButton` | botões com visual próprio |
| `PinosTeste` | classe aninhada estática, `extends JComponent` | fileira de LEDs do último teste |
| `Resultado` | `record` | uma linha do ranking |
| `Fio`, `Fase` | classes aninhadas estáticas | dados de um fio e de uma fase |

## Conceitos de Java e POO usados

O mapa completo, com a linha de cada exemplo, está em [`docs/CONCEITOS-POO.md`](docs/CONCEITOS-POO.md). Em resumo:

- **Classes, objetos e construtores**, com sobrecarga de construtor e uso de `this` e `super`.
- **Herança** das classes do Swing (`JFrame`, `JPanel`, `JButton`, `JComponent`).
- **Polimorfismo**: sobrescrita de `paintComponent` em todas as telas e botões, variáveis de supertipo (`JButton` guardando `BotaoCabo`, `List` guardando `ArrayList`, `Shape` guardando `Path2D`) e vetores de supertipo.
- **Classes aninhadas, internas e anônimas**.
- **Record**, **coleções com genéricos** (`List`, `Map`), **lambdas**, **referências de método** e **stream**.
- **Tratamento de exceções** (`try`/`catch`/`finally`) e **arquivos** (`java.nio.file`).
- **Membros estáticos e constantes** (`static final`), vetores e matrizes.

## Relação com o conteúdo da disciplina

Esta seção compara o código com o conteúdo das aulas (slides, textos e formulários de exemplo da disciplina). Os números são as linhas de `RJ45Game.java`.

### 1. Conteúdo das aulas que está no jogo

| Tema das aulas | Onde aparece no código | Linha |
|---|---|---|
| Sintaxe básica: `main`, tipos, comentários | `public static void main(String[] args)`; atributos `int`, `boolean`, `float`, `String`, `char` | 2013 |
| Classe, objeto e `new` | `static class Fio { int id; int pino = -1; Rectangle casa; }` e `Fio fio = new Fio();` | 1275, 1393 |
| Referência e `null` | `Fio f = arrastando;` (duas variáveis, o mesmo objeto); `slot[p] == null` (pino vazio) | 1571, 1818 |
| Troca de mensagens e delegação | `dicaBtn.addActionListener(e -> board.usarDica());` (a janela repassa o pedido ao tabuleiro) | 208 |
| Construtor com o nome da classe e `super(...)` | `public RJ45Game() { super("Monte o cabo de rede - RJ45 / Cat5e"); ...` | 163, 164 |
| Sobrecarga de construtor e `this` | `BotaoCabo(String texto, Color cor) { this(texto, cor, false); }` e `this.cor = cor;` | 600, 603 |
| Construtor padrão (classe sem construtor declarado) | `Fio` e `Fase` não declaram construtor; `new Fio()` e `new Fase()` usam o padrão | 1393, 1345 |
| Herança (`extends`) | `public class RJ45Game extends JFrame`; `static class BotaoCabo extends JButton`; `class Board extends JPanel` | 35, 597, 1280 |
| Sobreposição com `@Override` | `@Override protected void paintComponent(Graphics g0)` em cada tela e botão | 612, 1771 |
| Upcasting | `final JButton dicaBtn = new BotaoCabo("", new Color(255, 210, 60), true);` | 140 |
| Vetor de supertipo percorrido no mesmo laço | `for (JComponent c : new JComponent[]{dicasBox, dicaBtn, nova, ...})` | 171 |
| Composição (um dos pilares) | `final Board board = new Board();` (a janela tem um tabuleiro) | 146 |
| Modificadores `public`, `protected` e default | `public` na classe, no construtor e no `main`; `protected` em `paintComponent`; o restante sem modificador | 35, 612 |
| `final` | `static final int[] T568B = {...}`; `final Color cor;` | 48, 598 |
| Escopo: atributo, parâmetro e variável local | em `soltar(Point p)`: `arrastando` é atributo, `p` é parâmetro, `melhor` é local | 1569 a 1572 |
| Laço `for` e operadores | `for (int i = 0; i < 8; i++) ok[i] = slot[i].id == padrao[i];` | 1599 |
| `equals` | `tela.equals(JOGO)`; `r.nome().equalsIgnoreCase(jogador)` | 251, 1722 |
| Collections: `List` e `Map` | `List<Resultado> lista = new ArrayList<>();`; `Map<String, Runnable> atalhos = new HashMap<>();` | 461, 154 |
| Hierarquia de exceções | `catch (IOException e)`; `catch (RuntimeException ignorada)` | 467, 465 |
| Objeto sem referência (garbage collector) | `fases = novasFases();` (as fases da partida anterior ficam sem referência); `arrastando = null;` | 1366, 1571 |
| `import` de pacote inteiro e de uma classe | `import javax.swing.*;` e `import java.util.List;` | 6, 18 |
| Nome completo em vez de `import`, e classes de mesmo nome em pacotes diferentes | `new javax.swing.Timer(...)` (existe também `java.util.Timer`); `java.util.List` × `java.awt.List` | 250, 18 |
| Pacote default (classe sem `package`) | o arquivo não declara `package`, como no `FormAluno` da aula prática | 6 |
| AWT + Swing juntos | `import javax.swing.*;` e `import java.awt.*;` | 6, 9 |
| Classe que estende `JFrame`, com as 4 partes do formulário (atributos, construtor, eventos, `main`) | `RJ45Game` | 139, 163, 207, 2013 |
| Métodos do `JFrame` | `setDefaultCloseOperation(EXIT_ON_CLOSE);` `pack();` `setLocationRelativeTo(null);` `setVisible(true)` | 242, 243, 245, 2015 |
| `SwingUtilities.invokeLater` | `SwingUtilities.invokeLater(() -> { new RJ45Game().setVisible(true); });` | 2014 |
| Componentes: `JLabel`, `JButton`, `JTextField`, `JPasswordField`, `JCheckBox`, `JPanel` | `faseLbl`, `dicaBtn`, `campo`, `senha`, `dicasBox`, `raiz` | 141, 140, 1010, 1011, 139, 153 |
| Evento de botão com lambda | `nova.addActionListener(e -> board.novaPartida());` | 209 |
| Layouts `FlowLayout`, `BorderLayout`, `GridLayout` | `new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4))`; `new JPanel(new BorderLayout())`; `new GridLayout(3, 1)` | 174, 232, 188 |
| Bordas com `BorderFactory` | `BorderFactory.createCompoundBorder(...)`, `createLineBorder`, `createEmptyBorder` | 203 |
| `JOptionPane.showConfirmDialog` | `JOptionPane.showConfirmDialog(this, "Voltar ao menu inicial? ...", "Menu inicial", JOptionPane.YES_NO_OPTION)` | 304, 423 |
| Diálogo modal com `JDialog` | `JDialog d = new JDialog(this, titulo, true);` | 355 |
| `Integer.parseInt` | `Integer.parseInt(p[1])` (converte o texto lido do arquivo do ranking) | 450 |
| Validação de campo com `trim()`, `isEmpty()` e `return` | `if (s.isEmpty()) { erro.setText("O nome não pode ficar vazio."); return; }` | 432, 1107 |
| Operador condicional (`? :`) | `return usouDicas ? pontos : pontos * 2;` | 1690 |

### 2. Conteúdo das aulas que o jogo não usa

| Tema das aulas | Situação no código |
|---|---|
| `private` e getters/setters próprios | não há `private`; os atributos têm acesso default. O único método de acesso é `boolean admin()` (linha 311) |
| Declaração `package` | não há; a classe fica no pacote default |
| Interface própria e `implements` | não há; o jogo só usa interfaces da biblioteca, por lambda |
| Classe abstrata própria | não há; o jogo estende `AbstractAction`, da biblioteca (linha 263) |
| Sobrecarga de método comum | só existe sobrecarga de construtor (`BotaoCabo`) |
| `instanceof` | não aparece |
| `while` e `do while` | todos os laços são `for` |
| `Set`, `Comparable` e `equals` sobrescrito à mão | não aparecem; a ordenação usa `Comparator` e o `equals` é o gerado pelo `record` |
| `showMessageDialog`, `showInputDialog`, `Float.parseFloat` | não aparecem; as mensagens vão para o rodapé (`status.setText`) e o nome é lido em um `JTextField` |
| `JRadioButton`, `ButtonGroup`, `JComboBox`, `JList`, `JTextArea` | não aparecem |
| `DISPOSE_ON_CLOSE`, `setLayout(null)` e `setBounds` | não aparecem; o diálogo é fechado com `d.dispose()` (linha 398) |
| Classe com `implements ActionListener` | não há; os eventos usam lambda, como nos formulários da aula prática |
| `System.out.println` | não aparece; os avisos de erro usam `System.err.println` (linha 468) |
| Diagrama UML | o repositório não tem diagrama de classes |

### 3. O que vai além do conteúdo das aulas

**Observação:** os recursos abaixo não fazem parte do conteúdo das aulas e foram feitos com auxílio do Claude.

| Recurso | Onde aparece no código | Linha |
|---|---|---|
| `record` | `record Resultado(String nome, int pontos, ...)` | 440 |
| Classes aninhadas estáticas, internas e anônimas | `static class BotaoCabo`; `class Board`; `new MouseAdapter() { ... }` | 597, 1280, 1330 |
| Bloco estático e métodos utilitários `static` | `static { temaEscuro(); }`; `static void pintarCabo(...)` | 111, 521 |
| Lambdas fora de `addActionListener`, e referências de método | `atalho(JOGO, "T", board::iniciarTeste);`; `removerDoRanking(r::equals, true)` | 215, 1211 |
| Interfaces funcionais `Runnable`, `Predicate` e `Comparator` | `lista.sort(Comparator.comparingInt(Resultado::pontos).reversed().thenComparingInt(Resultado::tempo));` | 470, 487 |
| Stream | `carregarRanking().stream().filter(...).count()` | 1197 |
| Tratamento com `try` / `catch` / `finally` (as aulas trazem só a hierarquia) | `senhaAdm` | 90 a 100 |
| Arquivos com `java.nio.file` e ranking em CSV | `Files.readAllLines`, `Files.writeString`, `Files.copy` | 464, 476, 500 |
| Hash de login e senha (SHA-256 e PBKDF2), comparação em tempo constante e senha em `char[]` apagada da memória | `loginAdm`, `senhaAdm`, `mesmoHash` | 75 a 101 |
| Desenho próprio com Java 2D | `Graphics2D`, `Shape`, `Path2D`, `CubicCurve2D`, `GradientPaint`, `BasicStroke`, `AlphaComposite` | 521, 541, 1771 |
| Animações com `javax.swing.Timer` | `new javax.swing.Timer(30, e -> { ... }).start();` | 250, 1297 |
| Arrastar e soltar com eventos de mouse | `MouseAdapter` com `mousePressed`, `mouseDragged`, `mouseReleased` | 1330 |
| Atalhos de teclado | `InputMap`, `ActionMap`, `KeyStroke`, `AbstractAction` | 259 a 270 |
| Layouts `CardLayout`, `BoxLayout` e `GridBagLayout` | `final CardLayout cartas = new CardLayout();` | 152, 383, 1017 |
| Tabela do ranking | `JTable`, `DefaultTableModel`, `DefaultTableCellRenderer`, `JScrollPane` | 1217 a 1265 |
| Tema escuro pelo `UIManager` | `UIManager.put(tema[i], tema[i + 1]);` | 136 |
| Diálogo com visual próprio | `int perguntar(String titulo, ...)` com `d.setUndecorated(true)` | 354 |
| Texto em HTML dentro de `JLabel` | `new JLabel("<html><div style='width:350px'>...")` | 1199 |
| Matriz (vetor de vetores) | `static final int[][] PADRAO_FASE = {T568A, T568B, T568B};` | 57 |
| Utilitários da biblioteca | `StringBuilder`, `String.format`, `String.join`, `LocalDate`, `Collections.shuffle` | 70, 442, 1391, 1724 |

## Estrutura do repositório

```
.
├── RJ45Game.java           código do jogo, comentado linha a linha
├── README.md               este arquivo
├── docs/
│   ├── CONCEITOS-POO.md    conceitos de Java/POO presentes no código, com as linhas
└── .gitignore              ignora os .class gerados pelo javac
```

