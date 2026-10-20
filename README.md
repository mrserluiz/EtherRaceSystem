# EtherRaceSystem V0.1.1

Plugin Paper com Java 21. Seis raças iniciais: Humano (1), Elfo (1.09375), Fada (0.78125), Anão (0.90625), Draconato (1) e Tiefling (1).
As proporções usam a referência artística de 32 px para escala 1; não representam uma altura física exata de 2 blocos.
Altera escala e hitbox, sem trocar skins, adicionar asas ou modelos. Habilidades raciais ainda não implementadas.

## Instalação
Baixe o artefato da execução bem-sucedida mais recente em Actions, extraia o JAR, desligue o servidor e substitua o JAR anterior. Preserve a pasta EtherRaceSystem e reinicie.
Quem ainda não tem raça recebe o menu cerca de 2 segundos após entrar. /raca reabre o menu.
A configuração antiga continua funcionando; novas opções têm valores padrão mesmo sem regenerá-la:

```yaml
selecao:
  abrir_ao_entrar: true
  atraso_ticks: 40
```

## Debug (OP ou etherracesystem.admin)
| Comando | Resultado |
|---|---|
| /raca debug info [jogador] | Versão, raça salva, teste ativo, transformação, escala base, efetiva e quantidade de modificadores |
| /raca debug testar fada [jogador] | Testa tamanho sem alterar raça salva |
| /raca debug restaurar [jogador] | Encerra teste e restaura tamanho da raça salva |
| /raca debug menu [jogador] | Abre seleção para quem ainda não escolheu |
| /raca set <jogador> <raça> | Define raça permanentemente |
| /raca limpar <jogador> | Remove raça E condição sobrenatural para nova seleção |

IDs: humano, elfo, fada, anao, draconato, tiefling.
Teste temporário termina ao restaurar, selecionar uma raça, sair ou reiniciar. A transformação existente continua participando do cálculo da escala.
Sem jogador informado, debug atua no próprio executor; console deve informar jogador.
Para conferir a aparência, use terceira pessoa (F5) ou observe com outro jogador. Humano, Draconato e Tiefling têm o mesmo tamanho.
Diagnóstico mostra modificadores de outros plugins; diferenças entre escala base e efetiva podem explicar conflitos.
A visualização pelo Geyser/Bedrock requer validação no servidor.

## Transformações adicionais
Lobisomem e Vampiro preservam a raça base. Itens identificados por PDC, consumidos no clique direito.
- /raca item <jogador> <lobisomem|vampiro>
- /raca transformar <jogador> <lobisomem|vampiro>
- /raca info [jogador]
- /raca reload

Dados em jogadores.yml. Poderes, fraquezas e modelos ainda não implementados.

## Validação
A compilação Maven é executada pelo GitHub Actions. Testar no servidor: entrada sem raça, escolha e persistência após reconexão; tamanhos fada/elfo em terceira pessoa; debug sem mudar raça salva; restauração; morte e troca de mundo; Java e Bedrock.
