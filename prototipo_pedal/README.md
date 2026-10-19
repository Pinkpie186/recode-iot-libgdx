# ReCode: campanha com três fases

Esta versão abre um menu e executa **Circuitos Básicos**, **Rede Instável** e
**Sistema Central** na mesma janela. O ESP32 envia a contagem do mini pedal por USB serial; cada
volta válida avança ELO. Após a conclusão de uma fase, a próxima começa
automaticamente. A leitura do pedal acompanha toda a campanha, e o resultado
final mantém voltas, objetivos, pares regulares e tempo de movimento de cada
fase. Os números são parâmetros lúdicos provisórios, não metas de exercício.
Há também uma simulação identificada para desenvolvimento. O protótipo ainda
depende de validação do dispositivo físico e dos objetivos de reabilitação.

## Materiais

- Mini pedal ergométrico.
- ESP32 com cabo USB de dados.
- Ímã pequeno e chave reed normalmente aberta (ou módulo reed com saída digital
  de 3,3 V; nesse caso, conferir a ligação específica do módulo).
- Suporte e fixação para manter o ímã na parte giratória e a chave na parte fixa,
  sem contato entre eles e sem interferir no uso do pedal.

## Ligação proposta da chave reed simples

| Ponto | Ligação |
| --- | --- |
| Uma ponta da chave | GPIO 27 do ESP32 |
| Outra ponta da chave | GND do ESP32 |

O firmware usa `INPUT_PULLUP`: a passagem do ímã fecha a chave e leva o pino a
GND. **Não aplicar 5 V ao GPIO do ESP32.** A fixação deve resistir ao movimento
sem expor fios ou peças soltas. A distância entre ímã e sensor depende do modelo
e deve ser ajustada por teste manual, com o pedal parado.

## Firmware

Abra `firmware/recode_pedal/recode_pedal.ino` na Arduino IDE com a placa ESP32
configurada. Ajuste `SENSOR_PIN` se usar outro GPIO. Grave no ESP32 e abra o
monitor serial em **115200 baud**. Uma volta deve incrementar `cycle` uma vez.
O ESP32 envia uma linha JSON a cada 100 ms, inclusive quando o pedal está parado:

```json
{"v":1,"seq":42,"ms":123456,"mode":"pedal","cycle":7,"lastCycleMs":122900,"state":"ok"}
```

`cycle` é cumulativo desde a inicialização; `lastCycleMs` marca a última volta
para estimar RPM a partir do intervalo entre voltas. `state: ok` confirma a execução do
firmware e a transmissão, **não** detecta cabo solto na chave reed. A comparação
manual de voltas é necessária para verificar a montagem.

## Aplicativo desktop

O aplicativo está em `desktop/` e usa libGDX 1.14.2 e jSerialComm 2.11.4.
As folhas originais de ELO, do mascote e de Nó Zero ficam em
`desktop/src/main/resources/sprites/`. Cada folha tem 16 quadros em uma grade
4 × 4; o recorte ocorre no libGDX, sem alterar os PNGs. Nesta primeira fase,
ELO usa repouso, movimento e restauração; o mascote flutua, acompanha e orienta.
Nó Zero aparece na segunda e na terceira fase, com animações de presença,
interferência e enfraquecimento.
Os três cenários em `desktop/src/main/resources/scenarios/` representam as fases
definidas no GDD: a câmara de circuitos em azul/ciano, a rede fragmentada em
azul e magenta e o núcleo central em violeta. O cenário de Circuitos Básicos
aparece na primeira fase jogável; Rede Instável usa o segundo cenário na fase 2.
Sistema Central usa o terceiro cenário na fase 3.
É preciso instalar um JDK 17 e Gradle, pois este pacote não inclui um wrapper
nem binários. No terminal, dentro de `desktop/`:

```text
gradle run -Pport=COM3
```

Substitua `COM3` pela porta exibida para o ESP32 no seu computador. O programa
não escolhe uma porta automaticamente. Na primeira mensagem válida ele estabelece
o ponto de partida; as voltas **seguintes** avançam ELO. Isso evita pontuar
voltas anteriores à abertura do jogo.

### Trabalhar antes de ter as peças

**Teste interativo no Windows:** abra `Jogar-ReCode.cmd` com dois cliques. No
menu, pressione `Enter` para jogar. Cada toque em `F` simula uma pedalada; toque
em intervalos regulares para carregar o escudo nas fases 2 e 3. `Espaço` abre a
pausa. Este teste requer JDK 17 e as dependências do Gradle já baixadas. Também
é possível iniciar pela pasta `desktop/` com `gradle run -Pkeyboard=true`.

Com JDK 17 e Gradle instalados, abra `desktop/` no terminal e execute:

```text
gradle run -Psimulate=true
```

O modo de simulação gera uma volta por segundo (60 RPM) e alimenta **o mesmo
parser e contador** usados pelo ESP32. A tela identifica a simulação. Ele
permite conferir as três fases e as transições enquanto as peças não chegam.
Na primeira fase, observe os bloqueios mudarem de vermelho para verde nas
voltas 5, 10 e 15. Após a volta 20, a campanha passa para Rede Instável;
depois segue para Sistema Central. A simulação não demonstra controle via IoT e não atende
sozinho ao requisito de avaliação do projeto. Para testar o dispositivo real,
feche a simulação e use `-Pport=COM3` com a porta correta.

**Menu e controles:** use as setas ou clique para escolher uma opção e `Enter`
para confirmar. O menu permite iniciar uma campanha, continuar a tentativa
atual, ver as instruções e sair. Durante a campanha, `Espaço`, `P` ou `Esc`
abre a pausa com as opções **Continuar**, **Voltar ao menu** e **Nova campanha**.
`M` também volta ao menu enquanto a pausa está aberta. `R` inicia uma nova
campanha após vitória ou derrota. Voltas recebidas durante a pausa ou a
transição são descartadas. A leitura de RPM serve somente para
informar movimento detectado; não define intensidade segura ou meta de
reabilitação. A conclusão de uma fase não pontua dados recebidos depois da meta.
Ao voltar ao menu, **Continuar campanha** preserva a fase, a integridade e o
desempenho da tentativa.

O fundo percorre a arte conforme ELO avança, com fragmentos e estruturas em
planos de movimento diferentes. Há três fragmentos de energia por fase: ELO os
recolhe ao chegar a cada trecho, e a campanha exibe o total de 0 a 9. Os
circuitos, nós e condutos continuam reagindo às pedaladas.

Na Rede Instável e no Sistema Central, **três pares seguidos de pedaladas
regulares** carregam o escudo do mascote. Ele protege ELO dos ataques de Nó Zero
por **15 segundos de jogo**; a contagem para durante a pausa e no menu. O
indicador mostra a carga ou o tempo restante. Um ataque bloqueado gera um pulso
ciano; um impacto sem proteção faz ELO tremer, aciona a animação de dano e
reduz a integridade.

### Progressão, vitória e derrota

O jogo começa em Circuitos Básicos. Cada conclusão abre a próxima fase após
uma breve tela de resultado. Ao restaurar os três escudos do Sistema Central,
o jogador vence. A integridade começa em 3 e é mantida entre as fases. Na Rede
Instável, após oito voltas em um nó sem concluir a sequência regular, e no Sistema
Central, após nove voltas em um escudo sem completar as três etapas, cada seis
voltas extras sem estabilização causam
uma interferência de Nó Zero e reduzem a integridade em 1. Integridade zero
encerra a tentativa em derrota. Não há limite de tempo nem meta de velocidade;
um ritmo lento e regular não causa dano. O resultado final mantém as métricas
das três fases até o jogador iniciar uma nova tentativa.

Para conferir a lógica de progressão sem abrir a janela do jogo, execute
`gradle verifyCampaign` em `desktop/`.

### Fase 2 — Rede Instável

Para abrir somente esta fase durante o desenvolvimento, sem as peças IoT:

```text
gradle run -Pphase=2 -Psimulate=true
```

Com o mini pedal ligado, substitua a simulação pela porta serial correta:

```text
gradle run -Pphase=2 -Pport=COM3
```

ELO atravessa a rede e estabiliza **três nós**. Cada nó recebe pelo menos oito
voltas válidas e dois pares **seguidos** de intervalos regulares. A comparação é com o ritmo
imediatamente anterior do próprio jogador, com tolerância provisória de 35%; não
existe meta de velocidade. Voltas fora desse intervalo continuam registradas, e
o total de voltas e os nós já recuperados não são apagados; apenas a sequência
do nó atual recomeça quando o intervalo muda. Se faltarem pares regulares depois da oitava
volta, o nó permanece aberto para recuperação sem limite de tempo. Esses valores
são parâmetros lúdicos a validar, não critérios clínicos.

**Nó Zero** aparece no cenário e provoca uma interferência visual enquanto o
sinal do nó atual não está estável. Não há flashes ou punição por ritmo baixo.
Quando os três nós são estabilizados, a campanha segue para o Sistema Central.
No modo de fase isolada, `R` reinicia a fase; na campanha, a integridade e os
resultados anteriores continuam. A navegação exclusivamente por IoT ainda
depende da decisão sobre controles físicos e testes com o dispositivo.

### Fase 3 — Sistema Central

Para jogar sem as peças IoT:

```text
gradle run -Pphase=3 -Psimulate=true
```

Com o mini pedal conectado:

```text
gradle run -Pphase=3 -Pport=COM3
```

No confronto final, ELO restaura **seis condutos** e estabiliza **três escudos**
de Nó Zero. Cada escudo pede a sequência: **precisão** (três voltas detectadas
individualmente), **ritmo** (dois pares de intervalos regulares seguidos) e
**estabilidade** (mais três pares regulares seguidos). O jogo mostra a etapa
ativa e o progresso. Após a precisão, Nó Zero desvia a energia para outra rota;
ELO a acompanha automaticamente. Nó Zero fica no plano distante da arena,
acompanha a câmera e aparece atrás das estruturas e rotas do primeiro plano.
Na campanha, os disparos saem de sua boca, seguem a posição de ELO e só exibem
o impacto quando chegam ao personagem ou ao escudo. Ele enfraquece a cada
escudo restaurado e desaparece na vitória.

O jogador não precisa aumentar a velocidade. Voltas em lote continuam no total,
mas não comprovam uma volta individual nem um intervalo regular. Uma mudança de
ritmo reinicia apenas a sequência da etapa atual; escudos já restaurados
permanecem. O pedal mede voltas e intervalos, então a precisão e a coordenação
são representadas por esse sinal de forma provisória. Os números e a tolerância
de 35% são parâmetros lúdicos, não critérios clínicos.

Após os três escudos, a campanha mostra a vitória e um resumo de todas as
fases. O parâmetro `phase` permite abrir fases isoladas para desenvolvimento;
sem esse parâmetro, o jogo inicia a campanha completa.

### Visualizar os cenários sem jogar

Sem precisar do pedal, execute um comando de cada vez na pasta `desktop/`:

```text
gradle run -Ppreview=rede
gradle run -Ppreview=central
```

Cada comando abre apenas a arte da fase indicada, com o aviso de prévia visual.
As prévias servem apenas para visualizar a arte. Para jogar a campanha, execute
`gradle run -Psimulate=true` ou use a porta do ESP32.

## Verificação na bancada

1. Com o mini pedal parado, confirmar que `cycle` permanece constante.
2. Girar a manivela lentamente 10 voltas completas e comparar com o aumento de
   `cycle`. Repetir em ritmo confortável e observar contagens duplicadas.
3. Abrir o jogo, esperar a indicação de conexão e completar 5 voltas; conferir
   que ELO avança 5 unidades de reparo.
4. Desconectar o USB: o jogo deve indicar perda de dados em até 2 s e parar de
   avançar. Reconectar exige reiniciar o aplicativo nesta primeira versão.

O limiar de rejeição de pulsos abaixo de 180 ms e a meta de 20 voltas são
parâmetros **de engenharia para teste**, não metas de exercício. A duração, a
intensidade e a adequação do movimento de pedalada para reabilitação continuam
pendentes de validação profissional.

## Escopo após este protótipo

Adicionar calibração de cadência individual, menu/pausa/reinício controlados por
IoT, métricas persistidas entre execuções e tratamento de reconexão.
Validar as três fases na tela e no dispositivo real antes de considerar a
integração concluída.
