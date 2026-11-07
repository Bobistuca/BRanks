# BRanks

Plugin de **ranks e tags** para Minecraft (Spigot, Paper, Purpur, Bukkit), compilado para a API 1.8.8 e feito para rodar da **1.8 até as versões mais novas**.
É um jar só, **sem dependências**: não precisa de LuckPerms (mas funciona junto com ele, veja abaixo).

## Instalação
1. Coloque `BRanks-1.0.0.jar` na pasta `plugins/` do servidor.
2. Inicie o servidor. Serão criados `config.yml`, `ranks.yml`, `tags.yml` e `jogadores.yml` em `plugins/BRanks/`.

## Conceitos
- **Rank**: grupo do jogador (prefixo, sufixo, cores, permissões). Cada jogador tem 1 rank.
- **Tag**: enfeite extra que o jogador escolhe (`/tag vip`). Pode exigir uma permissão.
- **Hierarquia sem números**: a **ordem** dos ranks no `ranks.yml` é a hierarquia (primeiro = mais alto). Use `/rank mover`.
- **Cores**: `&a`, `&l`, nomes (`vermelho`, `azul`, `dourado`, `red`...) ou hex `#FF8800` (1.16+; em versões antigas vira a cor mais próxima).

## Comandos
| Comando | O que faz |
|---|---|
| `/rank lista` | Lista os ranks |
| `/rank info [jogador]` | Mostra rank e tag |
| `/rank definir <jogador> <rank>` (ou `/setrank`) | Define o rank. Use `padrao` no lugar do rank para resetar |
| `/rank criar <nome>` / `apagar <rank>` | Cria / apaga um rank |
| `/rank nome <rank> <texto>` | Nome de exibição |
| `/rank prefixo <rank> <texto\|nenhum>` | Prefixo |
| `/rank sufixo <rank> <texto\|nenhum>` | Sufixo |
| `/rank cor <rank> <cor>` | Cor do nome do jogador |
| `/rank corchat <rank> <cor>` | Cor da mensagem no chat |
| `/rank permissao <rank> <add\|remover\|lista> [perm]` | Permissões do rank |
| `/rank herda <rank> <add\|remover> <outro>` | Herdar permissões de outro rank |
| `/rank padrao <rank>` | Rank dos novos jogadores |
| `/rank mover <rank> <cima\|baixo>` | Muda a hierarquia |
| `/rank recarregar` | Recarrega os arquivos |
| `/tag lista` / `/tag <nome>` / `/tag remover` | Jogador escolhe sua tag |
| `/tag definir <jogador> <tag\|nenhuma>` (ou `/settag`) | Força a tag de alguém |
| `/tag criar <nome> <texto>` / `apagar <tag>` | Cria / apaga uma tag |
| `/tag texto <tag> <texto>` | Muda o texto da tag |
| `/tag permissao <tag> <perm\|nenhuma>` | Permissão necessária para usar a tag |

## Permissões
- `branks.admin` (op): gerenciar ranks e tags.
- `branks.tag.usar` (todos): escolher tags.
- `branks.chat.cores` (op): usar `&` de cor no chat.
- Em `ranks.yml`: `"*"` dá todas as permissões e `-permissao` remove uma.

## Configuração por arquivo
`ranks.yml`:
```yaml
vip:
  nome: VIP
  prefixo: "&6[VIP]"
  sufixo: ""
  cor: dourado
  cor-chat: ""
  padrao: false
  herda: [membro]
  permissoes:
    - branks.tag.vip
```
`tags.yml`:
```yaml
vip:
  texto: "&6[VIP]"
  permissao: branks.tag.vip   # vazio = todos podem usar
```
O `config.yml` controla o formato do chat, da tab e das mensagens (todas em português e editáveis).
Placeholders: `{jogador} {tag} {prefixo} {sufixo} {rank} {cor} {corchat} {mensagem}`. Os espaços entre tag, prefixo e sufixo são automáticos.

## Usando junto com LuckPerms
Se você prefere que o LuckPerms cuide das permissões, coloque `permissoes.ativar: false` no `config.yml`.
O BRanks continua cuidando de prefixo, tag, chat e tab. Para a permissão de uma tag, basta dar a permissão dela (ex.: `branks.tag.vip`) pelo LuckPerms.
Se outro plugin já formata o chat, use `chat.ativar: false`.

## Compilar
Com Maven: `mvn package` (o jar sai em `target/`).
Sem Maven: baixe o `spigot-api-1.8.8` e rode
`javac -encoding UTF-8 --release 8 -cp spigot-api.jar -d classes src/main/java/com/branks/*.java`, copie `src/main/resources/*.yml` para `classes/` e `jar cf BRanks-1.0.0.jar -C classes .`.
