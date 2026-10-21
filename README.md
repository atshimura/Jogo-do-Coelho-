# Coelhitos

Coelhitos é um jogo de plataforma 2D para Android, em orientação horizontal. O primeiro marco entrega uma fase jogável numa planície gramada, com controles por toque, cenouras, baús de poderes, inimigos, corações e portal de saída.

## Construir

Requer JDK 17, Android SDK com plataforma 35 e Gradle 8.9 ou compatível.

```sh
gradle assembleDebug
```

APK de depuração: `app/build/outputs/apk/debug/app-debug.apk`.

## Documentação

- [Especificação funcional](docs/especificacao.md)
- [Etapas e andamento](docs/andamento.md)
- [Notas da versão 0.1.2](docs/release-v0.1.2.md)
- [Notas da versão 0.1.3](docs/release-v0.1.3.md)
- [Notas da versão 0.2.0](docs/release-v0.2.0.md)
- [Notas da versão 0.2.1](docs/release-v0.2.1.md)
- [Notas da versão 0.1.1](docs/release-v0.1.1.md)
- [Notas da versão 0.1.0](docs/release-v0.1.0.md)

## Direção visual e sonora

Visual 2D lateral original, alegre e colorido, com formas arredondadas e ambientes de fantasia. Referência de clima e acabamento: jogos de plataforma Kirby para Nintendo Switch, em especial *Kirby’s Return to Dream Land Deluxe*. Personagens, cenários, música e efeitos são próprios; não são usados recursos da Nintendo.

Trilha e efeitos são sintetizados no próprio aplicativo, sem arquivos externos ou dependências de áudio.

## Identidade visual do aplicativo

O ícone Android Coelhitos está incluído em recursos adaptativos (Android 8+) e nos tamanhos tradicionais para compatibilidade com versões anteriores. A tela inicial, HUD e controles foram redesenhados com painéis translúcidos, tipografia arredondada, ícones vetoriais próprios e cores consistentes.

