# TODO fTab

- Cabeçalho/rodapé: sem placeholders (ex.: jogadores online) nem mensagens rotativas.
- Unificar a lista grupo -> tag com a do fChat (hoje duplicada).
- Tags do fTags no tab (`TagsApi.getVisibleTags(uuid, TagSurface.TAB)`).
- Limite de 16 caracteres do 1.8 no prefixo e no nome da equipa (tags longas são cortadas); só 99 regras.
- Jogador numa equipa de outro plugin não recebe a tag de grupo (fica na equipa dele); sem tratamento de conflitos.
- Equipas `NN_` guardadas no `scoreboard.dat` após crash/kill ou depois de mudar a config ficam vazias e nunca são limpas (só se limpam as do plano atual).
