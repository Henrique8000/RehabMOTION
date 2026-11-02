# RehabMOTION

Jogo sério para **reabilitação da amplitude de movimento do punho**, feito em Java com libGDX
para a disciplina de Jogos Digitais da Universidade Presbiteriana Mackenzie.

Um potenciômetro preso ao punho funciona como um goniômetro: mede o ângulo de flexão e extensão e
o Arduino envia esse valor pela USB. O jogador conduz um barco por um rio só com o movimento do
punho, desviando de pedras e troncos e pegando moedas. As moedas perto das margens exigem levar o
punho até o limite do movimento, e cada fase mede a amplitude que o paciente realmente usou.

**Autores**

- Henrique Flávio Guimarães (RA 10427920)
- Lucas Zeferino Baracat (RA 10396267)

---

## O jogo

São **3 fases** de 60 segundos com dificuldade crescente. Para vencer, é preciso chegar ao fim do
rio com a meta de moedas; bater 3 vezes afunda o barco.

| Fase | Cenário | Velocidade do rio | Intervalo entre obstáculos | Moedas perto da margem | Meta de moedas |
|---|---|---|---|---|---|
| 1 | Rio | 150 | 1,3 s | 50% | 8 |
| 2 | Floresta tropical | 190 | 1,05 s | 65% | 12 |
| 3 | Vulcão | 230 | 0,85 s | 80% | 16 |

- Moeda comum vale 10 pontos; moeda com brilho, perto da margem, vale 20.
- Antes de cada fase aparece uma introdução com o objetivo e uma contagem de 5 segundos.
- Ao fim de cada fase aparecem pontos, moedas, batidas e a **amplitude usada** (em % e em graus).
- Ao vencer a fase 3 aparece a tabela **"Evolução nas fases"**, comparando as três fases.

### Métricas de reabilitação

| Métrica | O que indica |
|---|---|
| Amplitude calibrada (graus) | o alcance máximo do paciente naquela sessão |
| Amplitude usada por fase (% e graus) | quanto desse alcance foi usado durante o jogo |
| Moedas perto da margem | quantas vezes o punho chegou perto do limite |
| Batidas | controle e precisão do movimento |

---

## Hardware

| Item | Observação |
|---|---|
| Arduino Uno (pode ser clone) | clones com chip CH340 precisam do driver CH340 no Windows |
| Potenciômetro linear 10k (B10K) | |
| 3 jumpers | |
| Cabo USB do Arduino | |
| 2 hastes (réguas, palitos ou papelão grosso) e velcro | para montar o goniômetro no punho |

### Ligações

```
Potenciômetro (de frente, pernas para baixo)

   esquerda   meio    direita
      |        |        |
     5V       A0       GND
```

Qualquer um dos pinos GND do Arduino serve. Se o barco andar para o lado errado, aperte **I** na
tela de teste para inverter o sentido.

### Montagem no punho

1. Prenda o corpo do potenciômetro na ponta de uma haste, presa no **antebraço**.
2. Prenda o eixo do potenciômetro na outra haste, presa no **dorso da mão**.
3. Deixe o eixo bem em cima da articulação do punho.
4. Ao dobrar o punho, uma haste gira em relação à outra e o eixo gira junto.

### Arduino

1. Abra `arduino/controle_potenciometro/controle_potenciometro.ino` na Arduino IDE.
2. Em **Ferramentas**, escolha a placa **Arduino Uno** e a porta (por exemplo, COM3).
3. Clique em **Carregar**.
4. Para conferir, abra **Ferramentas > Plotter Serial** a 9600: a linha sobe e desce ao girar.
5. **Feche o Plotter e o Monitor Serial antes de abrir o jogo**: a porta só aceita um programa por vez.

O Arduino envia uma linha por leitura, só com o número de 0 a 1023 (média de 4 leituras, a cada 20 ms):

```
512
518
530
```

---

## Como rodar

Requer JDK 17 ou mais novo.

```bash
# Windows
gradlew.bat lwjgl3:run

# Linux / macOS
./gradlew lwjgl3:run
```

- Com o Arduino ligado, o jogo encontra a porta sozinho.
- Sem o Arduino, o jogo usa o **teclado** para simular o sensor (setas ou A/D).
- Para forçar uma porta: `gradlew.bat lwjgl3:run --args="COM3"`.
- Para gerar o executável: `gradlew.bat lwjgl3:jar` (fica em `lwjgl3/build/libs/`).

### Controles

**Tela de teste e calibração**

| Tecla | Ação |
|---|---|
| ENTER | começar o jogo na fase 1 |
| C | começar / terminar a calibração |
| R | voltar à calibração padrão |
| I | inverter o sentido |
| TAB | trocar entre Arduino e teclado |
| ESC | sair |

**Durante o jogo**

| Tecla | Ação |
|---|---|
| Punho (ou setas / A e D) | mover o barco |
| ENTER | pular a introdução; próxima fase; tentar de novo |
| P | pausar / continuar |
| ESC | voltar para a tela de teste |

### Calibração

Aperte **C**, leve o punho até o extremo de um lado e depois do outro, e aperte **C** de novo.
Essa faixa passa a ser a largura inteira do rio, então cada paciente joga com a amplitude que
consegue alcançar. A amplitude calibrada, em graus, aparece na tela e é usada nos resultados.

---

## Organização do código

```
core/src/main/java/br/mackenzie/
├── Main.java                     cria o controle, troca as telas e guarda os resultados
├── game/
│   ├── Phase.java                as 3 fases e a dificuldade de cada uma
│   └── PhaseResult.java          resultado de uma fase vencida
├── input/
│   ├── MotionInput.java          interface do controle usada pelo jogo
│   ├── AbstractMotionInput.java  suavização, calibração, ângulo e posição
│   ├── ArduinoMotionInput.java   leitura do Arduino pela USB (jSerialComm)
│   ├── KeyboardMotionInput.java  simulação do sensor pelo teclado
│   └── MotionInputs.java         escolhe entre Arduino e teclado
└── screens/
    ├── MotionTestScreen.java     tela de teste e calibração
    ├── GameScreen.java           o jogo: obstáculos, moedas, fases e resultados
    ├── RiverArt.java             desenho do rio, das margens e do barco
    └── Theme.java                cores de cada cenário
lwjgl3/.../Lwjgl3Launcher.java    abre a janela no computador
arduino/controle_potenciometro/   código do Arduino
```

O jogo só conversa com a interface `MotionInput`. O valor principal é `getPosition()`, de **-1**
(um extremo calibrado) a **+1** (o outro). Por isso o Arduino e o teclado são intercambiáveis sem
mudar nada no jogo.

Se os dados do Arduino param por 1 segundo, o jogo mostra "desconectado" e tenta reconectar sozinho.

---

## Tecnologias

- Java e [libGDX](https://libgdx.com/) 1.13.1
- [jSerialComm](https://fazecast.github.io/jSerialComm/) 2.11.4 para a comunicação serial
- Gradle
- Arduino Uno

> Projeto acadêmico. Em uso real, a amplitude de movimento e a duração das sessões devem ser
> definidas com orientação de um fisioterapeuta.
