# Coelhitos 0.5.2

## Salto

A altura padrão do salto direcional foi restaurada; o pulo vertical também preserva seu impulso anterior. A altura maior fica restrita ao poder Supersalto.

## Inimigos

A fase tem cinco formas visuais: criatura azul pequena, criatura verde com casco, criatura roxa de corpo alongado e cauda móvel, criatura dourada com chifres e ave verde-azulada com cachecol. Inimigos terrestres mantêm os pés no terreno, alternam as patas e acompanham o passo com o corpo. As aves batem as asas.

## Fase e colisões

Foram acrescentados morros e rampas retas íngremes, com mais variedade no percurso. O nível agora reúne 22 inimigos terrestres, 8 voadores e 14 obstáculos. Uma colisão bloqueia o avanço; pressionar contra o mesmo ponto não reaplica dano até o coelho sair do contato.

## Validação

A execução [#48](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39828135547) passou no GitHub Actions: compilação Gradle e 13 testes instrumentados no emulador Android API 35. A suíte verifica controles, saltos, evasão, composição da fase, quadros de caminhada, bloqueio de colisão e ausência de dano repetido enquanto o contato continua.

APK debug `Coelhitos-v0.5.2-debug.apk`, `versionCode 12`, `versionName 0.5.2`. SHA-256: `8a1904be5ec82eea12111d2351cf07da080cda443f8bae3e2258dc3e9c3b6ed8`.

Artefato APK `Coelhitos-debug-apk` (`12447866823`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12447122967`, retenção de 14 dias), anexados à execução #48.
