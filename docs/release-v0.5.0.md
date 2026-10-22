# Coelhitos 0.5.0

## Cenário e relevo

O cenário recebeu morros em camadas, luzes e sombras suaves, árvores e vegetação mais volumétricas, com acabamento colorido e macio para combinar com o coelho. Há duas subidas retas e íngremes ao longo da planície.

## Inimigos

A fase agora tem 18 inimigos terrestres e 6 voadores. Alguns terrestres têm cauda animada; os voadores usam cachecol e flutuam acima do chão. Os terrestres acompanham a altura do terreno, mantêm as patas apoiadas, fazem pequenos movimentos de caminhada e piscam.

## Colisões

As áreas de colisão seguem melhor os sprites de inimigos e obstáculos. Ao tocar em um deles, o coelho para no ponto de contato e aplica a regra de dano vigente. Um salto cronometrado ainda permite passar por cima sem dano.

## Validação

A execução [#44](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39808475411) passou no GitHub Actions: build Gradle e 12 testes instrumentados em emulador Android API 35. Foram exercitados os controles, evasão cronometrada, subida, composição de inimigos e colisões bloqueantes. O Logcat não registrou exceção fatal.

APK debug `Coelhitos-v0.5.0-debug.apk`, `versionCode 10`, `versionName 0.5.0`; SHA-256: `4040aa1b5381cd4c99b6e256423b10b039d5ad2c1eda42ffc5a6a532171c9e89`.

Artefato `Coelhitos-debug-apk` (`12438762708`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12439206467`, retenção de 14 dias), anexados à execução #44.
