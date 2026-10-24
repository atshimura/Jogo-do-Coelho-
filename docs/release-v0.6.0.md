# Coelhitos 0.6.0

## Fase 2: caverna de cristais

A jornada continua ao entrar no portal da fase 1. A segunda fase acontece em uma caverna colorida com cristais luminosos, terreno jogável com subidas e descidas, espinhos, fogo e cinco buracos. O portal final encerra a jornada atual.

### Jogabilidade

- A transição preserva corações, poder equipado e munição de cenoura.
- Cinco baús sorteiam poderes segundo as chances já definidas.
- Quedas em buracos têm animação, removem exatamente um coração e devolvem o coelho a um local seguro antes do vão. O dano de queda consome coração mesmo que haja poder equipado.
- O atirador vegetal dispara sementes com antecipação visual; pular permite evitar o disparo.
- Os novos inimigos incluem o besouro rochoso baixo e a mariposa lilás de quatro asas.
- Marcha dos inimigos terrestres, asas da mariposa e animação de queda são animadas.
- Música da caverna e efeitos de semente e queda são sintetizados no próprio aplicativo.

## Validação

A versão inclui testes instrumentados para controles, poderes, portal, progressão entre fases, composição da caverna, queda e retorno seguro, disparos de sementes e animação das asas. Registrar abaixo o resultado final do GitHub Actions e o SHA-256 do APK gerado.

- `versionCode`: 13
- `versionName`: 0.6.0
- Testes: 18 testes instrumentados passaram; smoke test no emulador passou, sem exceção fatal ou `ConcurrentModificationException`.
- GitHub Actions: [execução #54](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/40100495114), conclusão `success`, Android Emulator API 35.
- Artefato APK `Coelhitos-debug-apk`: ID `12563220098`, tamanho compactado 10,8 MB, retenção de 30 dias. Evidência `Coelhitos-emulator-evidence`: ID `12562946094`, retenção de 14 dias.
- SHA-256 do APK: `8f9ebe72579aae83afbbb469c8dc80274b1ee10323b33f1ea616fb420e4dcf70`.

A duração de cerca de três minutos é o alvo de design. O workflow valida os sistemas e o trecho de jogo automatizado, mas uma jogada completa deve ser cronometrada em aparelho físico para confirmar o alvo.
