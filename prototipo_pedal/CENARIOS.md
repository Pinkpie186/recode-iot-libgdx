# Cenários de ReCode

Os três fundos abaixo foram criados com a ferramenta integrada de geração de
imagens em 13/10/2026. São artes de cenário, sem personagens, texto ou interface.
O jogo desenha ELO, o mascote, a trilha e os indicadores por cima do fundo.

| Fase | Arquivo | Uso atual |
| --- | --- | --- |
| Circuitos Básicos | `desktop/src/main/resources/scenarios/circuitos_basicos.png` | Fundo da primeira fase jogável |
| Rede Instável | `desktop/src/main/resources/scenarios/rede_instavel.png` | Prévia visual |
| Sistema Central | `desktop/src/main/resources/scenarios/sistema_central.png` | Prévia visual |

As imagens têm 1672 × 941 pixels. O jogo as ajusta à janela preservando a
proporção. Os cenários 2 e 3 não implicam que a jogabilidade dessas fases está
pronta. Os objetivos e parâmetros de exercício continuam sujeitos à validação
indicada no GDD.

## Prompts usados

### Circuitos Básicos

> Use case: stylized-concept. Asset type: seamless-feeling full-screen 16:9 background for a 2D side-scrolling libGDX game, phase 1 'Circuitos Básicos' of ReCode. Create a polished crisp pixel-art digital-world environment: a quiet dark navy circuit chamber with simple geometric architecture, orderly cyan and teal luminous pathways, sparse square data particles, three distant repairable circuit structures suggested in the background. Match the color and sharp pixel-cluster aesthetic of a bright cyan energy protagonist sprite, but DO NOT depict any character. Wide horizontal composition at approximately 1536x864. Keep the central horizontal band from 35% to 65% height relatively dark, uncluttered, and low contrast so a moving character, track, and HUD drawn by the game stay legible. Put architectural detail mainly toward edges and top/bottom. Gentle depth by distinct pixel-art layers, clean silhouettes, no blur, no photorealism, no gradients, no UI, no text, no labels, no numbers, no logos, no borders, no watermark. Opaque background.

### Rede Instável

> Use case: stylized-concept. Asset type: full-screen 16:9 background for phase 2 'Rede Instável' of ReCode, a 2D side-scrolling libGDX pixel-art game. Create a dark digital network environment made of suspended data bridges, fractured circuit lines, misaligned nodes and moderate visual interference. The palette is deep navy and indigo with controlled cyan and magenta signals, clearly a progression from a simpler clean cyan circuit chamber. Wide horizontal composition approximately 1536x864; coherent side-view game environment with visual depth in crisp pixel clusters. Keep the central horizontal band from 35% to 65% height darker, quieter, and usable for a character, interactive track and HUD drawn by code. Put broken architecture mostly near edges and upper/lower layers. Instability should be readable but not visually overwhelming, no flashing pattern, no aggressive red, no characters, no enemies, no UI, no text, no labels, no logos, no watermark, no blur, no gradients, no photorealism. Opaque background.

### Sistema Central

> Use case: stylized-concept. Asset type: full-screen 16:9 background for phase 3 'Sistema Central' of ReCode, a 2D side-scrolling libGDX pixel-art game. Design the interior of an immense digital core chamber where restoration and corruption meet: monumental radial processor architecture in the distance, concentric circuit conduits, angular data pylons, deep violet and midnight navy machinery with restrained cyan restoration light and magenta corruption accents. It should feel like the final location and be visually distinct from a clean cyan circuit room and a fractured network, yet part of the same crisp hard-edged pixel-art world. Wide approximately 1536x864 side-view game environment; visual details mostly toward edges, ceiling, and floor, with a relatively dark quiet central horizontal band from 35% to 65% height for a moving character, game track, boss sprite, and HUD added separately by code. No characters, no villain, no sprite, no UI, no text, no labels, no numbers, no logos, no watermark, no blur, no photorealism, no gradients, no flashing pattern. Opaque background.
