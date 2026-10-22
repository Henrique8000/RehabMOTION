# RehabMOTION (libGDX + Arduino)

Jogo sério para reabilitação de **amplitude de movimento** (punho ou joelho).
Um potenciômetro preso à articulação mede o ângulo; o Arduino envia esse valor
pela USB e o jogador controla um barquinho que desvia de obstáculos no rio.

Estado atual: **camada de controle pronta** + **tela de teste** (o barco segue o
movimento, com calibração). As fases, o menu e os obstáculos são os próximos passos.

---

## 1. Materiais (baratos e fáceis de achar)

| Item | Observação |
|---|---|
| Arduino Uno ou Nano (pode ser clone) | o grupo talvez já tenha um |
| Potenciômetro linear **10k (B10K)** | o mais comum; custa poucos reais |
| 3 jumpers (fios) e protoboard | protoboard é opcional |
| Cabo USB do Arduino | |
| 2 réguas de plástico, palitos grandes ou papelão grosso | para montar o "goniômetro" |
| Velcro ou elástico + fita / cola quente | para prender no braço ou perna |

> Clones com chip **CH340** precisam do driver CH340 no Windows.

## 2. Ligações

```
Potenciômetro (vista de frente, pernas para baixo)

   esquerda   meio    direita
      |        |        |
     5V       A0       GND
```

Se o barco andar para o lado errado, aperte **I** no jogo (ou troque os fios das pontas).

## 3. Montagem no corpo ("goniômetro caseiro")

Exemplo para **flexão e extensão do punho**:

1. Cole o **corpo** do potenciômetro na ponta de uma régua; essa régua vai presa no **antebraço**.
2. Prenda o **eixo** (pode colar um botão/knob nele) na outra régua; essa vai presa no **dorso da mão**.
3. O eixo do potenciômetro fica exatamente em cima da articulação do punho.
4. Ao dobrar o punho, as réguas giram uma em relação à outra e o eixo gira junto.

Para o **joelho** é a mesma ideia, com réguas mais compridas (coxa e canela).

## 4. Arduino

1. Abra `arduino/controle_potenciometro/controle_potenciometro.ino` na IDE do Arduino.
2. Escolha a placa e a porta, e envie (**Carregar**).
3. Teste em **Ferramentas > Plotter Serial** (9600): ao mexer, a linha sobe e desce.
4. **Feche o Plotter/Monitor Serial** antes de abrir o jogo: a porta só aceita um programa.

## 5. Rodar o jogo

```bash
# Windows
gradlew.bat lwjgl3:run

# Linux / macOS
./gradlew lwjgl3:run
```

Ou abra a pasta no IntelliJ IDEA (ele reconhece o Gradle) e rode `Lwjgl3Launcher`.

- Com o Arduino ligado, o jogo o encontra sozinho.
- Sem o Arduino, o jogo usa o **teclado** (setas ou A/D) para simular o sensor.
- Para forçar uma porta: `gradlew lwjgl3:run --args="COM3"`.
- Executável (.jar): `gradlew lwjgl3:jar` → fica em `lwjgl3/build/libs/`.

### Teclas da tela de teste

| Tecla | Ação |
|---|---|
| C | começar / terminar a calibração |
| R | voltar à calibração padrão |
| I | inverter o sentido |
| TAB | trocar entre Arduino e teclado |
| ESC | sair |

**Calibrar:** aperte C, mova a articulação até o extremo de um lado e depois do outro,
e aperte C de novo. Essa faixa passa a ser a "tela inteira" do barco, então cada
paciente usa a amplitude que consegue alcançar. A amplitude calibrada (em graus)
já é uma métrica de evolução para o relatório.

---

## 6. Como o código está organizado

```
core/src/main/java/br/mackenzie/
├── Main.java                     cria o controle e a primeira tela
├── input/
│   ├── MotionInput.java          INTERFACE: o que o jogo sabe sobre o controle
│   ├── AbstractMotionInput.java  parte comum: suavização, calibração, ângulo, posição
│   ├── ArduinoMotionInput.java   lê o Arduino pela USB (jSerialComm)
│   ├── KeyboardMotionInput.java  simula o sensor com o teclado
│   └── MotionInputs.java         escolhe Arduino ou teclado
└── screens/
    └── MotionTestScreen.java     tela de teste com o barquinho
lwjgl3/.../Lwjgl3Launcher.java    abre a janela no computador
arduino/controle_potenciometro/   código do Arduino
```

O jogo só usa a interface `MotionInput`. O valor principal é `getPosition()`,
de **-1** (extremo esquerdo) a **+1** (extremo direito):

```java
MotionInput input = game.getInput();
float x = centroDoRio + input.getPosition() * larguraMaxima;
```

Por isso dá para desenvolver tudo com o teclado e trocar para o Arduino sem mudar
nenhuma linha do jogo (polimorfismo, como no Lab 6).

### Protocolo Arduino → jogo

Uma linha de texto por leitura, só o número de 0 a 1023, a 9600 baud:

```
512
518
530
```

Linhas inválidas são ignoradas. Se os dados param por 1 segundo, o jogo mostra
"desconectado" e tenta reconectar sozinho.

---

## 7. Próximos passos

- [ ] Obstáculos e itens no rio (itens perto das margens incentivam a amplitude)
- [ ] 3 fases com dificuldade progressiva (velocidade, alcance pedido, duração)
- [ ] Menu principal (iniciar, pausar, reiniciar) **controlado pelo sensor**:
      por exemplo, segurar o movimento para um lado por 1 segundo para escolher
      (o enunciado pede controle exclusivo pelo dispositivo; o teclado fica só
      para desenvolvimento e para a tela de ajuste)
- [ ] Métricas por fase: amplitude alcançada, tempo nos extremos, itens coletados,
      suavidade do movimento; salvar histórico para mostrar a evolução
- [ ] Sprites e sons

> Projeto acadêmico: as faixas de movimento e a duração das sessões devem ser
> definidas com orientação de um fisioterapeuta.
