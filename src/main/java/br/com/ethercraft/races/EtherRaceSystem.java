package br.com.ethercraft.races;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

public final class EtherRaceSystem extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String[] RACAS = {"humano","elfo","fada","anao","draconato","tiefling"};
    private static final String[] TRANSFORMACOES = {"lobisomem","vampiro"};
    private final Map<UUID, String> testes = new HashMap<>();
    private NamespacedKey chave;
    private File arquivo;
    private YamlConfiguration dados;

    @Override public void onEnable() {
        saveDefaultConfig();
        chave = new NamespacedKey(this, "transformacao");
        arquivo = new File(getDataFolder(), "jogadores.yml");
        dados = YamlConfiguration.loadConfiguration(arquivo);
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("raca")).setExecutor(this);
        Objects.requireNonNull(getCommand("raca")).setTabCompleter(this);
        for (Player p : Bukkit.getOnlinePlayers()) prepararEntrada(p);
    }
    @Override public void onDisable() { salvar(); }
    private void salvar() { try { dados.save(arquivo); } catch (IOException e) { getLogger().severe("Falha ao salvar jogadores.yml: " + e.getMessage()); } }
    private String path(Player p, String campo) { return p.getUniqueId() + "." + campo; }
    private String raca(Player p) { return dados.getString(path(p,"raca"), ""); }
    private String transformacao(Player p) { return dados.getString(path(p,"transformacao"), ""); }
    private boolean valido(String valor, String[] opcoes) { return valor != null && Arrays.asList(opcoes).contains(valor); }
    private String nomeRaca(String id) { return getConfig().getString("racas." + id + ".nome", id); }
    private void aplicar(Player p) {
        var atributo = p.getAttribute(Attribute.SCALE);
        if (atributo == null) { getLogger().warning("Atributo SCALE indisponível para " + p.getName()); return; }
        String r = testes.getOrDefault(p.getUniqueId(), raca(p)), t = transformacao(p);
        double escala = r.isEmpty() ? 1.0 : getConfig().getDouble("racas." + r + ".escala", 1.0);
        if (!t.isEmpty()) escala *= getConfig().getDouble("transformacoes." + t + ".escala_multiplicador", 1.0);
        atributo.setBaseValue(Math.max(0.0625, Math.min(16, escala)));
    }
    private void definir(Player p, String r) {
        testes.remove(p.getUniqueId());
        dados.set(path(p,"raca"),r); salvar(); aplicar(p);
        p.sendMessage("§aSua raça agora é §e" + nomeRaca(r) + "§a.");
    }
    private void limpar(Player p) {
        testes.remove(p.getUniqueId());
        dados.set(path(p,"raca"),null); dados.set(path(p,"transformacao"),null);
        salvar(); aplicar(p); p.sendMessage("§eSua raça e transformação foram removidas.");
    }
    private ItemStack icone(String r) {
        Material m = Material.matchMaterial(getConfig().getString("racas." + r + ".material", "PAPER"));
        ItemStack item = new ItemStack(m == null ? Material.PAPER : m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + nomeRaca(r));
        meta.setLore(List.of("§7Escala: " + getConfig().getDouble("racas." + r + ".escala", 1.0), "§7Clique para escolher esta raça"));
        item.setItemMeta(meta); return item;
    }
    private void menu(Player p) {
        if (!raca(p).isEmpty()) { p.sendMessage("§cVocê já escolheu uma raça. Solicite alteração a um administrador."); return; }
        Inventory inv = Bukkit.createInventory(null, 9, "§5EtherCraft • Escolha sua raça");
        for (int i=0; i<RACAS.length; i++) inv.setItem(i, icone(RACAS[i]));
        p.openInventory(inv);
    }
    private ItemStack itemTransformacao(String tipo) {
        String base = "transformacoes." + tipo + ".";
        Material mat = Material.matchMaterial(getConfig().getString(base + "material", "BONE"));
        ItemStack item = new ItemStack(mat == null ? Material.BONE : mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tipo.equals("lobisomem") ? "§6Dente de Lobisomem" : "§5Presas de Vampiro");
        meta.setLore(List.of("§7Relíquia sobrenatural rara", "§eClique com botão direito para transformar-se"));
        meta.setCustomModelData(getConfig().getInt(base + "custom_model_data"));
        meta.getPersistentDataContainer().set(chave, PersistentDataType.STRING, tipo);
        item.setItemMeta(meta); return item;
    }
    private void transformar(Player p, String tipo) {
        dados.set(path(p,"transformacao"),tipo); salvar(); aplicar(p);
        p.sendMessage("§dVocê adquiriu a condição sobrenatural: §f" + getConfig().getString("transformacoes." + tipo + ".nome", tipo) + "§d!");
    }
    private void prepararEntrada(Player p) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            aplicar(p);
            if (raca(p).isEmpty() && getConfig().getBoolean("selecao.abrir_ao_entrar", true)) {
                p.sendMessage("§eEscolha sua raça. Você também pode abrir a seleção com /raca.");
                menu(p);
            }
        }, Math.max(1L, getConfig().getLong("selecao.atraso_ticks", 40L)));
    }
    @EventHandler public void entrar(PlayerJoinEvent e) { prepararEntrada(e.getPlayer()); }
    @EventHandler public void sair(PlayerQuitEvent e) {
        if (testes.remove(e.getPlayer().getUniqueId()) != null) aplicar(e.getPlayer());
    }
    @EventHandler public void mudarMundo(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (e.getPlayer().isOnline()) aplicar(e.getPlayer());
        }, 2L);
    }
    private void diagnostico(CommandSender sender, Player p) {
        var a = p.getAttribute(Attribute.SCALE);
        sender.sendMessage("§6EtherRaceSystem " + getDescription().getVersion() + " | " + p.getName());
        sender.sendMessage("§7Raça salva: §f" + (raca(p).isEmpty() ? "Nenhuma" : nomeRaca(raca(p)))
            + " §7| Teste: §f" + testes.getOrDefault(p.getUniqueId(), "Nenhum"));
        sender.sendMessage("§7Transformação: §f" + (transformacao(p).isEmpty() ? "Nenhuma" : transformacao(p)));
        sender.sendMessage(a == null ? "§cAtributo SCALE indisponível." :
            "§7Escala base: §f" + a.getBaseValue() + " §7| Efetiva: §f" + a.getValue()
            + " §7| Modificadores: §f" + a.getModifiers().size());
    }
    @EventHandler public void renascer(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> aplicar(e.getPlayer()), 2L);
    }
    @EventHandler public void clicarMenu(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§5EtherCraft • Escolha sua raça")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() < 0 || e.getRawSlot() >= RACAS.length || !raca(p).isEmpty()) return;
        String id = RACAS[e.getRawSlot()]; p.closeInventory(); definir(p,id);
    }
    @EventHandler public void usarItem(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack item = e.getItem(); if (item == null || !item.hasItemMeta()) return;
        String tipo = item.getItemMeta().getPersistentDataContainer().get(chave, PersistentDataType.STRING);
        if (!valido(tipo, TRANSFORMACOES)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (raca(p).isEmpty()) { p.sendMessage("§cEscolha primeiro sua raça com /raca."); return; }
        if (!transformacao(p).isEmpty()) { p.sendMessage("§cVocê já possui uma transformação sobrenatural."); return; }
        transformar(p,tipo);
        item.setAmount(item.getAmount()-1);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { if(sender instanceof Player p) menu(p); else sender.sendMessage("Use /raca info <jogador>"); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("info")) {
            Player p = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player pl ? pl : null);
            if (p == null) { sender.sendMessage("§cJogador não encontrado."); return true; }
            sender.sendMessage("§6"+p.getName()+" §7| Raça: §f"+(raca(p).isEmpty()?"Nenhuma":nomeRaca(raca(p)))+" §7| Transformação: §f"+(transformacao(p).isEmpty()?"Nenhuma":transformacao(p))); return true;
        }
        if (!sender.hasPermission("etherracesystem.admin")) { sender.sendMessage("§cSem permissão."); return true; }
        if (sub.equals("debug")) {
            if (args.length < 2) {
                sender.sendMessage("§e/raca debug <info|testar|restaurar|menu> [raça] [jogador]");
                return true;
            }
            String acao = args[1].toLowerCase(Locale.ROOT);
            boolean testar = acao.equals("testar");
            int indice = testar ? 3 : 2;
            Player p = args.length > indice ? Bukkit.getPlayerExact(args[indice])
                : (sender instanceof Player pl ? pl : null);
            if (p == null) { sender.sendMessage("§cInforme um jogador online."); return true; }
            switch (acao) {
                case "info" -> diagnostico(sender, p);
                case "testar" -> {
                    if (args.length < 3 || !valido(args[2].toLowerCase(Locale.ROOT), RACAS)) {
                        sender.sendMessage("§eRaças: " + String.join(", ", RACAS)); return true;
                    }
                    testes.put(p.getUniqueId(), args[2].toLowerCase(Locale.ROOT));
                    aplicar(p);
                    p.sendMessage("§eTeste temporário de tamanho: " + nomeRaca(args[2].toLowerCase(Locale.ROOT))
                        + ". Use /raca debug restaurar para encerrar.");
                    diagnostico(sender, p);
                }
                case "restaurar" -> { testes.remove(p.getUniqueId()); aplicar(p); diagnostico(sender, p); }
                case "menu" -> menu(p);
                default -> sender.sendMessage("§eUse info, testar, restaurar ou menu.");
            }
            return true;
        }
        if (sub.equals("reload")) { reloadConfig(); for(Player p:Bukkit.getOnlinePlayers()) aplicar(p); sender.sendMessage("§aConfiguração recarregada."); return true; }
        if (args.length < 2) { sender.sendMessage("§e/raca set <jogador> <raca> | limpar <jogador> | item <jogador> <lobisomem|vampiro> | transformar <jogador> <tipo>"); return true; }
        Player alvo = Bukkit.getPlayerExact(args[1]); if(alvo == null) { sender.sendMessage("§cJogador não encontrado ou offline."); return true; }
        if (sub.equals("limpar")) { limpar(alvo); return true; }
        if (args.length < 3) { sender.sendMessage("§cInforme a raça ou transformação."); return true; }
        String tipo = args[2].toLowerCase(Locale.ROOT);
        if (sub.equals("set") && valido(tipo,RACAS)) { definir(alvo,tipo); return true; }
        if (sub.equals("transformar") && valido(tipo,TRANSFORMACOES)) { transformar(alvo,tipo); return true; }
        if (sub.equals("item") && valido(tipo,TRANSFORMACOES)) {
            HashMap<Integer,ItemStack> sobras=alvo.getInventory().addItem(itemTransformacao(tipo));
            sobras.values().forEach(it -> alvo.getWorld().dropItemNaturally(alvo.getLocation(),it));
            sender.sendMessage("§aItem entregue."); return true;
        }
        sender.sendMessage("§cComando ou identificador inválido."); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length==1) return (sender.hasPermission("etherracesystem.admin") ? Arrays.asList("info","set","limpar","item","transformar","reload","debug") : List.of("info"));
        if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("etherracesystem.admin")) {
            if (args.length == 2) return List.of("info", "testar", "restaurar", "menu");
            if (args.length == 3 && args[1].equalsIgnoreCase("testar")) return Arrays.asList(RACAS);
            if (args.length == (args[1].equalsIgnoreCase("testar") ? 4 : 3)) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if(args.length==3 && args[0].equalsIgnoreCase("set")) return Arrays.asList(RACAS);
        if(args.length==3 && (args[0].equalsIgnoreCase("item") || args[0].equalsIgnoreCase("transformar"))) return Arrays.asList(TRANSFORMACOES);
        return Collections.emptyList();
    }
}
