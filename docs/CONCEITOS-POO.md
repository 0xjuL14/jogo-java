# Conceitos de Java e POO no RJ45Game

Este documento lista os conceitos que aparecem de fato no código, com um exemplo de cada um e a linha onde ele está em `RJ45Game.java` (versão comentada). No código, os mesmos conceitos aparecem pelo nome no começo dos comentários: basta buscar por `herança`, `polimorfismo` etc.

Os números de linha valem para a versão comentada deste repositório. Se o arquivo for alterado, eles mudam.

## 1. Classe, objeto, atributo e método

Uma classe é o molde; um objeto é uma instância criada com `new`.

| Onde | Linha | O que mostra |
|---|---|---|
| `public class RJ45Game extends JFrame` | 35 | declaração da classe principal |
| `static class Fio { int id; int pino = -1; Rectangle casa; }` | 1275 | classe só com atributos (um deles com valor inicial) |
| `Fio fio = new Fio();` em `embaralharFios` | perto da 1391 | instanciação: são criados 8 objetos `Fio` por rodada |
| `new RJ45Game().setVisible(true);` | dentro do `main`, 2013 | cria a janela e chama um método dela |

## 2. Construtores, `this` e `super`

| Onde | Linha | O que mostra |
|---|---|---|
| `public RJ45Game()` | 163 | construtor: monta toda a interface |
| `super("Monte o cabo de rede - RJ45 / Cat5e");` | logo abaixo da 163 | `super(...)` chama o construtor da superclasse `JFrame` |
| `BotaoCabo(String texto, Color cor) { this(texto, cor, false); }` | 600 | **sobrecarga de construtor**: dois construtores com listas de parâmetros diferentes, e `this(...)` chamando um a partir do outro |
| `this.cor = cor;` em `BotaoCabo` | abaixo da 600 | `this` diferencia o atributo do parâmetro de mesmo nome |
| `RJ45Game.this::jogarNovamente` | 730 | `ClasseExterna.this`: a classe interna acessa o objeto da classe externa |

## 3. Herança

Todas as heranças do projeto são de classes do Swing. A relação é sempre "é um".

| Subclasse | Superclasse | Linha |
|---|---|---|
| `RJ45Game` | `JFrame` | 35 |
| `BotaoCabo` | `JButton` | 597 |
| `OpcaoPorta` | `JButton` | 631 |
| `PinosTeste` | `JComponent` | 674 |
| `TelaResultado` | `JPanel` | 705 |
| `TelaMenu` | `JPanel` | 831 |
| `TelaNome` | `JPanel` | 1008 |
| `TelaRanking` | `JPanel` | 1127 |
| `Board` | `JPanel` | 1280 |

A subclasse usa o que herdou sem reescrever. Exemplo: `OpcaoPorta` chama `getText()`, `isFocusOwner()` e `addActionListener(...)`, todos herdados de `JButton`.

## 4. Polimorfismo

O polimorfismo aparece de três formas.

**a) Sobrescrita (`@Override`)** — a subclasse redefine um método herdado, e o Swing chama a versão certa em tempo de execução. O principal é `paintComponent(Graphics)`: o Swing só sabe que tem um `JComponent` e manda "desenhe-se"; cada classe desenha de um jeito.

| Onde | Linha |
|---|---|
| `BotaoCabo.paintComponent` (com `super.paintComponent(g0)` no fim, reaproveitando o desenho do texto) | 626 |
| `OpcaoPorta`, `PinosTeste`, `TelaResultado`, `TelaMenu`, `TelaNome`, `TelaRanking`, `Board` | um `paintComponent` em cada classe |
| `isCellEditable` no modelo da tabela | abaixo da 1219 |
| `getTableCellRendererComponent` no renderizador da tabela | perto da 1249 |

**b) Variável de supertipo guardando objeto de subtipo**

| Onde | Linha |
|---|---|
| `final JButton dicaBtn = new BotaoCabo(...)` | 140 |
| `List<Resultado> lista = new ArrayList<>();` (interface e implementação) | 461 |
| `static Shape estrela(...)` devolve um `Path2D` como `Shape` | 327 |
| `JComponent pinos = new PinosTeste(ok.clone());` | 1655 |
| `pintarCabo(Graphics2D g, Shape caminho, ...)` aceita linha, curva ou caminho | 521 |

**c) Vetor de supertipo tratado de forma uniforme**

| Onde | Linha |
|---|---|
| `for (JComponent c : new JComponent[]{dicasBox, dicaBtn, nova, ...})` — caixa de seleção e botões no mesmo laço | 171 |
| `for (JTextField c : new JTextField[]{campo, senha})` — `JPasswordField` é subclasse de `JTextField` | 1042 |

**Sobrecarga** (mesmo nome, parâmetros diferentes, na mesma classe) aparece em um lugar só: os dois construtores de `BotaoCabo` (600). Não há métodos comuns sobrecarregados.

## 5. Encapsulamento e modificadores

| Onde | Linha | O que mostra |
|---|---|---|
| `boolean admin() { return ehAdmin; }` | 311 | método de acesso: as telas perguntam por ele em vez de ler o atributo |
| atributos `final` (`final Board board`, `final Color cor`) | 140 em diante, 597 em diante | referência que não pode ser trocada depois de criada |
| `record Resultado(...)` | 440 | dados imutáveis, lidos só pelos métodos de acesso gerados |
| hashes do login e da senha em vez do texto | 89 (`senhaAdm`) | ocultação de informação sensível |

Ponto de atenção para a apresentação: o código **não usa `private`**. Quase tudo tem o acesso padrão (de pacote), o que funciona porque todas as classes estão no mesmo arquivo. O encapsulamento existe por convenção (método `admin()`, atributos `final`, `record`), mas não é imposto pelo compilador. Trocar os atributos para `private` seria a melhoria natural.

## 6. Composição ("tem um")

A janela não herda das telas: ela **tem** um objeto de cada uma.

```java
final Board board = new Board();
final TelaMenu telaMenu = new TelaMenu();
final TelaNome telaNome = new TelaNome();
```

Do mesmo modo, `Board` tem um vetor `Fio[] fios`, um vetor `Fase[] fases` e dois `Timer`.

## 7. Classes aninhadas, internas e anônimas

| Tipo | Exemplo | Linha | Diferença |
|---|---|---|---|
| Aninhada estática | `static class BotaoCabo`, `Fio`, `Fase` | 597, 1275, 1278 | não depende de um objeto `RJ45Game` |
| Interna (sem `static`) | `class Board`, `class TelaMenu` | 1280, 831 | pertence a uma janela e acessa direto `jogador`, `status`, `mostrar(...)` |
| Anônima | `new JPanel(...) { @Override paintComponent ... }` | 188 | subclasse declarada e instanciada no mesmo lugar |
| Anônima de classe abstrata | `new AbstractAction() { ... }` | 263 | implementa o método abstrato `actionPerformed` |
| Anônima de adaptador | `new MouseAdapter() { ... }` | 842, 1330 | sobrescreve só os eventos de mouse que interessam |

## 8. Record

```java
record Resultado(String nome, int pontos, int tempo, int testes,
                 String fases, boolean dicas, String padrao, String data) { ... }
```

Linha 440. O compilador gera o construtor, os métodos de acesso (`nome()`, `pontos()`...), `equals`, `hashCode` e `toString`. O `equals` gerado é usado na exclusão de uma partida (`r::equals`, linha 1211). O record ainda tem um método de instância (`linha()`), um método estático de fábrica (`ler(String)`) e outro de formatação (`fasesTexto()`).

## 9. Membros estáticos e constantes

| Onde | Linha | O que mostra |
|---|---|---|
| `static final int[] T568A = {...}` | 49 | constante da classe, compartilhada por todos os objetos |
| `static { temaEscuro(); }` | 111 | bloco de inicialização estático: roda uma vez, quando a classe é carregada |
| `static void pintarCabo(...)`, `static String formatarTempo(int)` | 521 | métodos utilitários que não dependem de um objeto |
| `public static void main(String[] args)` | 2013 | ponto de entrada do programa |

## 10. Vetores e matrizes

| Onde | Linha |
|---|---|
| `static final int[][] PADRAO_FASE = {T568A, T568B, T568B};` — vetor de vetores | 57 |
| `Fio[] fios = new Fio[8];` — vetor de objetos | dentro de `Board`, 1280 em diante |
| `Fase[] novasFases()` — cria o vetor e depois um objeto para cada posição | 1343 |
| `int[] escolha = {-1};` — vetor de 1 posição para a lambda poder alterar o valor | dentro de `perguntar`, 354 |

## 11. Coleções e genéricos

| Onde | Linha |
|---|---|
| `Map<String, Runnable> atalhos = new HashMap<>();` | 154 |
| `List<Resultado>` com `ArrayList` | 461 |
| `List<Integer> ordem` + `Collections.shuffle(ordem)` (com autoboxing de `int` para `Integer`) | 1391 |
| `lista.sort(Comparator.comparingInt(Resultado::pontos).reversed().thenComparingInt(Resultado::tempo));` | 470 |

## 12. Interfaces funcionais, lambdas e referências de método

O projeto não declara interfaces próprias; ele **usa** interfaces da biblioteca.

| Interface | Exemplo | Linha |
|---|---|---|
| `ActionListener` | `dicasBox.addActionListener(e -> board.dicasAlteradas());` | 207 |
| `Runnable` | `atalho(JOGO, "T", board::iniciarTeste);` | 215 |
| `Predicate<Resultado>` | `removerDoRanking(Predicate<Resultado> alvo, boolean soUma)` | 487 |
| `Comparator<Resultado>` | ordenação do ranking | 470 |
| `TableCellRenderer` | lambda de 6 parâmetros no cabeçalho da tabela | 1249 |
| Stream | `carregarRanking().stream().filter(...).count()` | 1197 |

## 13. Tratamento de exceções

| Onde | Linha | O que mostra |
|---|---|---|
| `senhaAdm` | 89 a 98 | `try` / `catch` / `finally`: o `finally` apaga a senha da memória mesmo se houver erro |
| `carregarRanking` | 460 a 467 | `catch (IOException e)` para erro de leitura, e um `try` interno que ignora só a linha corrompida |
| `salvarResultado` | 474 a 478 | erro de gravação não derruba o jogo |
| `removerDoRanking` | 487 a 505 | devolve `-1` para sinalizar o erro a quem chamou |

## 14. Arquivos (persistência)

O ranking é um CSV na pasta do usuário, lido e gravado com `java.nio.file`:

- `Paths.get(System.getProperty("user.home"), ".rj45game_partidas.csv")` monta o caminho;
- `Files.readAllLines` lê;
- `Files.writeString(..., CREATE, APPEND)` acrescenta uma partida;
- `Files.copy(..., REPLACE_EXISTING)` faz a cópia `.bak` antes de excluir.

## 15. Programação orientada a eventos (Swing)

- **Listeners**: cliques (`addActionListener`), mouse (`MouseAdapter`) e teclado (`InputMap`/`ActionMap` no método `atalho`, linha 259).
- **Timers**: `javax.swing.Timer` para as animações e para o teste pino a pino (linha 1297).
- **Layouts**: `CardLayout` (troca de telas), `BorderLayout`, `FlowLayout`, `GridLayout`, `BoxLayout`, `GridBagLayout`.
- **Thread de eventos**: `SwingUtilities.invokeLater` no `main`.

## 16. O que o projeto não tem

Vale saber, caso o professor pergunte:

- **Interface própria** (`interface X`): não há. Só são usadas interfaces da biblioteca (`Runnable`, `Predicate`, `Shape`, `List`...).
- **Classe abstrata própria**: não há. O projeto estende uma classe abstrata da biblioteca (`AbstractAction`, linha 263).
- **Hierarquia própria de herança**: toda herança parte de classes do Swing; nenhuma classe do projeto herda de outra classe do projeto.
- **Modificador `private`** e **pacotes**: não são usados; tudo está em um arquivo, no pacote padrão.
- **Enum**: as telas e as fases são identificadas por `String` e `int`.

Uma evolução possível seria criar uma classe abstrata `Conector` com as subclasses `PlugueRJ45` e `Keystone`, cada uma sobrescrevendo a geometria dos pinos. Hoje essa diferença é resolvida com `if (keystone())` dentro de `Board`.
