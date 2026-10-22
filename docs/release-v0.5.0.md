# Coelhitos 0.5.0

## Cenário e relevo

O cenário recebeu morros em camadas, luzes e sombras suaves, árvores e vegetação mais volumétricas, com acabamento colorido e macio para combinar com o coelho. Há duas subidas retas e íngremes ao longo da planície.

## Inimigos

A fase agora tem 18 inimigos terrestres e 6 voadores. Alguns terrestres têm cauda animada; os voadores usam cachecol e flutuam acima do chão. Os terrestres acompanham a altura do terreno, mantêm as patas apoiadas, fazem pequenos movimentos de caminhada e piscam.

## Colisões

As áreas de colisão seguem melhor os sprites de inimigos e obstáculos. Ao tocar em um deles, o coelho para no ponto de contato e aplica a regra de dano vigente. Um salto cronometrado ainda permite passar por cima sem dano.

## Validação

Dois testes instrumentados novos verificam os tipos e a quantidade de inimigos, a subida reta e as colisões bloqueantes com inimigo e espinho. Os resultados, o APK e o SHA-256 serão registrados após a execução do CI.
