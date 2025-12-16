/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.MathUtils;

public final class DamageCmd extends Command
{
	public DamageCmd()
	{
		super("damage", "对自己造成指定的伤害",
			".damage <值>", "注: 值以半心表示",
			"示例: .damage 7 (施加3.5心)",
			"要造成更多伤害, 可以多次执行该命令");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			throw new CmdSyntaxError();
		
		if(MC.player.getAbilities().instabuild)
			throw new CmdError("创造模式下无法造成伤害");
		
		int amount = parseAmount(args[0]);
		applyDamage(amount);
	}
	
	private int parseAmount(String dmgString) throws CmdSyntaxError
	{
		if(!MathUtils.isInteger(dmgString))
			throw new CmdSyntaxError("不是数字: " + dmgString);
		
		int dmg = Integer.parseInt(dmgString);
		
		if(dmg < 1)
			throw new CmdSyntaxError("最低值为 1");
		
		if(dmg > 7)
			throw new CmdSyntaxError("最大值为 7");
		
		return dmg;
	}
	
	private void applyDamage(int amount)
	{
		Vec3 pos = MC.player.position();
		
		for(int i = 0; i < 80; i++)
		{
			sendPosition(pos.x, pos.y + amount + 2.1, pos.z, false);
			sendPosition(pos.x, pos.y + 0.05, pos.z, false);
		}
		
		sendPosition(pos.x, pos.y, pos.z, true);
	}
	
	private void sendPosition(double x, double y, double z, boolean onGround)
	{
		MC.player.connection
			.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround));
	}
}
