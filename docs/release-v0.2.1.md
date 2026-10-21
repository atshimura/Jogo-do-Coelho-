# Coelhitos 0.2.1

## Mascote do jogo

- O coelho dentro da fase agora usa a mesma identidade do ícone: pelagem branca fofa, olhos castanhos grandes, orelhas rosadas e cenoura.
- Criadas duas poses transparentes com o mesmo personagem: repouso quadrúpede e salto.
- A caminhada alterna o apoio e pequenos saltos com as patas traseiras pareadas. A pose de salto também é usada nos pulos direcionais.
- Mantidos o espelhamento ao virar, a sombra de contato, a perda de cor durante dano e a aura do poder Raio.

## Compatibilidade

- Android 6.0 ou superior (`minSdk 23`), em orientação horizontal.
- Versão do aplicativo: `0.2.1`, `versionCode 6`.

## Verificação

Compilação Gradle e teste em emulador Android API 35 concluídos com sucesso na [execução #24](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39593141098). Os comandos de movimento, os três tipos de salto e o disparo durante movimento foram exercitados; o app permaneceu aberto e o Logcat não registrou `FATAL EXCEPTION` nem `ConcurrentModificationException`. A captura final confirma a identidade do mascote na fase.

APK debug: `Coelhitos-v0.2.1-debug.apk`. SHA-256: `2b19058a62821209db73244c7bb39b24df050c74c2c7df68ecf1cea15ec43c5c`.

O teste automatizado percorreu um trecho curto; a fase completa e o uso em aparelho físico ainda precisam de avaliação manual.
