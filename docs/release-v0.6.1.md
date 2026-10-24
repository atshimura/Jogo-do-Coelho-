# Coelhitos 0.6.1

## Revisão das fases, menu e música

- A fase 1 agora oferece 24 cenouras: cinco perto do começo e 19 adicionais distribuídas ao longo do percurso. Cada cenoura continua valendo um disparo.
- As aves foram reposicionadas para não ocuparem o mesmo corredor de salto que os espinhos e o fogo. O nível mantém 30 inimigos.
- Na tela inicial, escolha Planície (fase 1) ou Caverna (fase 2) e pressione JOGAR FASE. Durante o jogo, INÍCIO abre o seletor de fase; começar novamente reinicia os itens e a vida.
- A trilha foi aprimorada com melodia, acompanhamento harmônico, baixo e ritmo sintetizados. Planície e Caverna tocam composições próprias.

## Validação

- `versionCode`: 14
- `versionName`: 0.6.1
- GitHub Actions [execução #56](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/40106779116) concluiu com sucesso: Gradle, Android Emulator API 35 e **21/21 testes instrumentados**.
- O Logcat do teste não apresentou `FATAL EXCEPTION` ou `ConcurrentModificationException`; a evidência capturou a tela de seleção e o gameplay com o botão INÍCIO visível.
- APK `Coelhitos-debug-apk`, artefato `12565112159`, retenção de 30 dias. Evidência `Coelhitos-emulator-evidence`, artefato `12565082262`, retenção de 14 dias.
- SHA-256 do APK: `f036a60bac368d86c82f97c3d07461fa2b87d6a88add76c8f4e26487d00eaa42`.
