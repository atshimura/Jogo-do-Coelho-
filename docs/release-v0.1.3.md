# Coelhitos 0.1.3

## Correções e melhorias

- O botão **AÇÃO** distingue toque curto de pressão longa: soltar após um toque curto dispara, inclusive enquanto o joystick mantém o coelho andando; manter pressionado com uma direção faz correr.
- O botão **PULO** e o comando de pulo do joystick agora usam um buffer breve. Um toque perto do pouso não se perde, e cada pressão inicia apenas um salto; a direção horizontal é capturada no momento do comando.
- Adicionado relevo suave ao coelho, aos inimigos e aos obstáculos, com sombras, contornos e gradientes de luz.
- Ao perder um coração, a trilha pausa brevemente e toca um efeito descendente de perda de vida. Perder somente um poder mantém o efeito de dano, sem interromper a música.

## Validação

- APK compilado como versão de depuração, identificador `com.coelhitos`, `versionCode 4` e `versionName 0.1.3`.
- O workflow do GitHub Actions instala o APK no emulador Android API 35, abre o jogo, envia gestos de toque, confirma que o processo permanece ativo e verifica o Logcat por exceções fatais e `ConcurrentModificationException`.
- O teste automatizado verifica inicialização e estabilidade durante um trecho curto; o tempo total de aproximadamente três minutos, a qualidade do áudio e a experiência de toque ainda devem ser conferidos em um celular físico.
