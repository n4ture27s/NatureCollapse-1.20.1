package n4ture27.nature_collapse.event;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import org.joml.Math;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class RandomEvent {
	public static RandomEvent instance = new RandomEvent();
	
	public Level level;
	public int timer = 100;
	
	public RandomEvent() {	
	}
	
	public void tick() {
		// ここに毎tickの処理を描く
		if (timer++ > 100) {
			timer = 0;
			
			var rand = Math.floor(Math.random() * 4);
			if (rand == 0){
				for (var player : level.players()) {
					player.sendSystemMessage(Component.literal("上からTNTが！！！！！！！"));
					
					Component.literal("item.minecraft.tnt"); // "item.minecraft.tnt"
					Component.translatable("item.minecraft.tnt"); // "TNT 火薬"
					
					var tntPos = player.position().add(0, 10, 0);
					var tnt = EntityType.TNT.create(level); // TNTエンティティを作成 まだスポーンしていない
					tnt.setPos(tntPos); // 位置を設定 スポーン準備ができた
					level.addFreshEntity(tnt); // 実際にスポーン
				}
			}
			if (rand == 1){
				for (var player : level.players()) {
					player.sendSystemMessage(Component.literal("ラッキー!らんまさんがダイヤを64個くれた!"));
					player.getInventory().add(new ItemStack(Items.DIAMOND,64));
				}
			}
			if (rand == 2){
				for (var player : new ArrayList<>(level.players())) { // level.players.forEach
					player.sendSystemMessage(Component.literal("うわぁ!らんまさんが殺してきた!"));
				}
			}
			if (rand == 3){
				
			}
		}
	}
}
