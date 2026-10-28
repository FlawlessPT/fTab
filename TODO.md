# TODO fTab

- Cabeçalho/rodapé: sem placeholders (ex.: jogadores online) nem mensagens rotativas.
- Unificar a lista grupo -> tag com a do fChat (hoje duplicada).
- Tags do fTags no tab (`TagsApi.getVisibleTags(uuid, TagSurface.TAB)`).
- Só 99 regras (prefixo das equipas `NN_`). O prefixo já não é cortado a 16; ver no teste manual o que o cliente 1.8 vê (ViaRewind). Prefixo vazio dá cor WHITE em vez de RESET; a atualização das equipas é O(n²) por ciclo; os prefixos podem faltar até `update-seconds` após um scoreboard novo.
- Jogador numa equipa de outro plugin não recebe a tag de grupo (fica na equipa dele); sem tratamento de conflitos.
- Equipas `NN_` guardadas no `scoreboard.dat` após crash/kill ou depois de mudar a config ficam vazias e nunca são limpas (só se limpam as do plano atual).
- Migração Paper 1.21: `setScoreboard` do fScoreboard sobrepõe o scoreboard de outro plugin; `TeamPlanTest` com linhas em branco a mais.

- Reload de config exige `/freload plugin` (plano de equipas, cabeçalho e período são fixados no arranque).
