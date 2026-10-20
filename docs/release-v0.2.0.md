# Coelhitos 0.2.0

## Revisão visual

- Menu de entrada, HUD e controles redesenhados para uma leitura mais clara e acabamento mais consistente.
- Corações, cenouras, botão de ação e botão de pulo usam desenhos próprios; o HUD não depende de emojis ou fontes especiais.
- Criado e integrado um ícone de aplicativo em recursos adaptativos e em cinco densidades tradicionais.
- Coelho de perfil recebeu uma animação de caminhada com pequenos saltos impulsionados pelas duas patas traseiras juntas.
- A fase ganhou morros e subidas que alteram o piso de caminhada, a posição dos itens, inimigos, obstáculos e do portal.
- Projétil de fogo agora tem silhueta e cores quentes evidentes. A rajada de penas tem haste e barbas visíveis.

## Compatibilidade

- Android 6.0 ou superior (`minSdk 23`).
- Orientação horizontal.
- Versão do aplicativo: `0.2.0`, `versionCode 5`.

## Verificação

GitHub Actions execução [#22, tentativa 2](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39516173225) concluiu `assembleDebug` e o roteiro no emulador Android API 35. O processo do jogo permaneceu ativo após o trecho testado. Logcat sem `FATAL EXCEPTION` nem `ConcurrentModificationException`; registros confirmaram salto vertical, salto para frente, salto para trás e disparo.

- APK debug: `Coelhitos-v0.2.0-debug.apk` (`versionCode 5`).
- SHA-256: `33dc6cec9cf5149323b130c52ec17d2c0fc2361de5a9be42931b91b9823f358d`.
- Artefato de emulador: `Coelhitos-emulator-evidence` (`12308053193`, 14 dias).
- Artefato APK: `Coelhitos-debug-apk` (`12308003118`, 30 dias).
- Limite: o teste automático cobre comandos e estabilidade por um trecho curto; não mede a fase inteira de aproximadamente três minutos nem substitui teste de áudio/ergonomia em aparelho físico.
