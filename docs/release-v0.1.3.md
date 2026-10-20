# Coelhitos 0.1.3

## Correções e melhorias

- O botão **AÇÃO** distingue toque curto de pressão longa: soltar após um toque curto dispara, inclusive enquanto o joystick mantém o coelho andando; manter pressionado com uma direção faz correr.
- O botão **PULO** e o comando de pulo do joystick agora usam um buffer breve. Um toque perto do pouso não se perde, e cada pressão inicia apenas um salto; a direção horizontal é capturada no momento do comando.
- Adicionado relevo suave ao coelho, aos inimigos e aos obstáculos, com sombras, contornos e gradientes de luz.
- Ao perder um coração, a trilha pausa brevemente e toca um efeito descendente de perda de vida. Perder somente um poder mantém o efeito de dano, sem interromper a música.

## Validação

- APK compilado como versão de depuração, identificador `com.coelhitos`, `versionCode 4` e `versionName 0.1.3`.
- SHA-256 do APK: `2505f37d36c6aad88b586cfd8f84607659962f62455f6d1477ec2afa921e38a8`.
- [Baixar APK debug](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39345854869/artifacts/12230369269) (artefato válido por 30 dias).
- GitHub Actions execução #17 passou no emulador Android API 35: instalação, abertura da fase, registros de salto para frente, para trás e vertical, disparo com movimento, processo ativo após 15 segundos e Logcat sem `FATAL EXCEPTION` ou `ConcurrentModificationException`.
- [Capturas e Logcat](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39345854869/artifacts/12230264401) (artefato válido por 14 dias).
- O teste automatizado cobre inicialização, os comandos principais e estabilidade durante um trecho curto; o tempo total de aproximadamente três minutos, a qualidade do áudio e a ergonomia em aparelho físico ainda devem ser conferidos manualmente.
