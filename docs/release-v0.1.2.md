# Coelhitos 0.1.2 — salto, animação e estabilidade

## Alterações

- Pulo sem direção sobe no lugar; direita + pulo avança à direita e esquerda + pulo salta à esquerda.
- O personagem vira para a direção indicada e mantém a orientação ao andar para trás.
- Animação quadrúpede refeita: as patas traseiras impulsionam pequenos pulos e as dianteiras acompanham o corpo.
- Reinício após perder três corações aguarda o fim das rotinas de colisão, evitando limpar listas durante a iteração.
- Saída de áudio e cancelamento de controles tratados com mais segurança.

## Verificação

- Package: `com.coelhitos`
- Version: 0.1.2 (`versionCode 3`)
- Requer Android 6.0 ou superior; orientação horizontal.
- APK debug: `Coelhitos-v0.1.2-debug.apk`
- SHA-256: `224b0e59042aecfb323deb126fe9a46bd6c7029b33362e23e75a1e54acb3b6e9`
- Fonte compilada com JDK 17; DEX gerado com D8; APK alinhado e assinatura verificada com ferramentas do Android SDK.
- A confirmação dos controles por toque em aparelho ou emulador ainda é necessária.
