# Coelhitos 0.4.1

## Entrada no portal

Ao se aproximar do portal enquanto avança, o coelho deixa de responder aos controles por um breve momento e caminha tranquilamente para dentro dele. O sprite encolhe durante a passagem; a tela de fase concluída só aparece depois que o coelho atravessa a entrada.

## Validação

Dez testes instrumentados passaram no GitHub Actions #40, em emulador Android API 35. O novo teste confirma o avanço até o portal e que a conclusão só ocorre após a entrada; os nove testes anteriores de poderes, evasão e controles também passaram. O Logcat não mostrou `FATAL EXCEPTION` nem `ConcurrentModificationException`.

- APK debug: `Coelhitos-v0.4.1-debug.apk` (`versionCode 9`, `versionName 0.4.1`).
- SHA-256: `1b4520857f13fb54ec2057e6c8e0c4456ee4e17d0e6da8087c43eb0eb06c2c75`.
- Artefato APK `Coelhitos-debug-apk` (`12389869066`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12390008829`, retenção de 14 dias).
- Execução: [GitHub Actions #40](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39695463171).
- O tempo integral da fase e a avaliação visual em aparelho físico ainda precisam de validação manual.
