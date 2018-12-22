package scripts;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

import org.tribot.api.input.Mouse;
import org.tribot.api2007.Camera;
import org.tribot.api2007.Game;
import org.tribot.api2007.GameTab;
import org.tribot.api2007.GameTab.TABS;
import org.tribot.api2007.Interfaces;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Options;
import org.tribot.api2007.Player;
import org.tribot.api2007.Players;
import org.tribot.api2007.Skills;
import org.tribot.api2007.Skills.SKILLS;
import org.tribot.api2007.Walking;
import org.tribot.api2007.types.RSCharacter;
import org.tribot.api2007.types.RSInterface;
import org.tribot.api2007.types.RSInterfaceChild;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSPlayer;
import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;

@ScriptManifest(authors = { "Java" }, category = "Combat", name = "java.PestControl")
public class PestControl extends Script implements Painting {

	int[] planks = { 14315, 25631, 25632 };
	int plankIndex = 0;

	String status = "Indle";

	boolean newGame = false;
	boolean inCombat = false;
	boolean died = false;

	int startPoints = -1;
	int currentPoints = 0;
	int pointsGained = 0;
	int startXp = 0;
	int currentXp = 0;

	Image logo = null;

	RSNPC target = null;

	long scriptStartTIME = System.currentTimeMillis();

	@Override
	public void run() {
		try {
			logo = this.getImage(new URL("https://i.ibb.co/1vBByXT/javaPC.png"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new Thread(new MouseControl()).start();
		new Thread(new PlayerStatus()).start();
		new Thread(new CameraControl()).start();
		new Thread(new DeathCapture()).start();
		startXp = this.getXp();
		while (true) {
			if (!inBoat() && !inGame()) {
				this.joinBoat();
			} else if (inBoat() && !inGame()) {
				RSInterface i = Interfaces.get(407, 6);
				if (i != null && i.isBeingDrawn() && i.getText() != null) {
					if (startPoints == -1)
						startPoints = Integer.parseInt(i.getText().split(":")[1].replace(" ", ""));
					currentPoints = Integer.parseInt(i.getText().split(":")[1].replace(" ", ""));
				}
				if (!GameTab.getOpen().equals(GameTab.TABS.INVENTORY)) {
					GameTab.open(GameTab.TABS.INVENTORY);
				}
				newGame = true;
				status = "In boat";
			} else if (inGame()) {
				if (newGame) {
					walkDown();
					newGame = false;
				}
				if (died) {
					status = "dying";
					for(int i = 0; i < random(6,8); i++) {
						if(!inGame())
							break;
						sleep(700,1000);
					}
					walkDown();
					died = false;
				}
				this.attack();
				sleep(500, 1000);
			} else {
				sleep(500, 1000);
			}
		}
	}

	public int getXp() {
		int amt = 0;
		for (SKILLS s : SKILLS.values()) {
			amt += s.getXP();
		}
		return amt;
	}

	public void walkDown() {
		status = "Running down";
		RSTile t = new RSTile(Player.getPosition().getX() + random(-10, 10),
				Player.getPosition().getY() - random(22, 30));
		for (int i = 0; i < random(4, 5); i++) {
			if (t.distanceTo(Player.getPosition()) < random(7, 13))
				break;
			Walking.walkPath(Walking.generateStraightPath(t));
			sleep(200, 300);
		}
		sleep(2000, 3500);
	}

	public boolean inCombatLoop() {
		if (Player.getRSPlayer().getInteractingCharacter() == null)
			return false;
		for (int i = 0; i < 20; i++) {
			if (Player.getRSPlayer().getAnimation() != -1) {
				return true;
			} else {
				sleep(100);
			}
		}
		return false;
	}

	public boolean inCombat() {
		return this.inCombat;
	}

	private void attack() {
		this.status = "Attacking";
		if (!inCombat()) {
			int yellowId = 1749;
			int blueId = 1748;
			int pinkId = 1750;
			int purpleId = 1747;
			int[] pids = { yellowId, blueId, pinkId, purpleId };
			RSNPC[] portals = NPCs.findNearest(pids);
			if (portals != null && portals.length > 0) {
				RSNPC p = portals[0];
				status = "Attacking portal";
				this.attackNPC(p);
			} else {
				RSNPC it = this.getBestMonster();
				if (it != null)
					this.attackNPC(it);
			}

		} else {
			this.status = "In combat";
		}
	}

	public RSNPC getBestMonster() {
		RSNPC[] npcs = NPCs.findNearest("Spinner", "Defiler", "Ravager", "Shifter", "Splatter", "Torcher");
		for (RSNPC it : npcs)
			if (it != null && (it.getHealthPercent() * 100) > random(20, 35))
				return it;
		return null;
	}

	public void attackNPC(RSNPC it) {
		if (it != null) {
			target = it;
			if (!it.isOnScreen() && it.getPosition().distanceTo(Player.getPosition()) > random(6, 7)) {
				this.status = "Walking to NPC";
				Walking.setWalkingTimeout(10000);
				Walking.walkPath(Walking.generateStraightPath(it.getPosition()));
				sleep(200, 300);
			}
			if (it != null) {
				it.click("Attack");
				if (random(0, 1) == 0)
					this.littleMouseMove();
				sleep(450, 1223);
			}
		}
	}

	public void joinBoat() {
		this.status = "Joining Boat";
		RSObject[] plank = Objects.findNearest(25, planks[plankIndex]);
		if (plank != null && plank.length > 0) {
			RSObject p = plank[0];
			if (plank != null) {
				for (int i = 0; i < random(5, 25); i++) {
					if (inBoat() || inGame())
						break;
					if (p.click("Cross")) {
						sleep(300, 1200);
						break;
					} else {
						sleep(200, 1000);
					}
				}
			}
		}
	}

	public void littleMouseMove() {
		Point nu = new Point(Mouse.getPos().x + random(-100, 100), Mouse.getPos().y + random(-100, 100));
		Mouse.move(nu);
	}

	public int random(int min, int max) {
		Random rand = new Random();
		int randomNum = rand.nextInt(max - min + 1) + min;
		return randomNum;
	}

	public boolean inBoat() {
		RSInterfaceChild i = Interfaces.get(407, 6);
		return i != null && !i.isHidden() && i.isBeingDrawn() && !this.inGame();
	}

	public boolean inGame() {
		RSInterfaceChild i = Interfaces.get(408, 6);
		return i != null && !i.isHidden() && i.isBeingDrawn();
	}

	public void rsDrawString(String s, int x, int y, Graphics g) {
		g.drawImage(logo, 545, 296, null);
		g.setColor(new Color(0, 0, 0, 155));
		g.fillRect(x - 3, y - 12, s.length() * 7, 16);
		g.setColor(new Color(Mouse.getPos().x / 3, Mouse.getPos().y / 3, (int) (Camera.getCameraRotation() * .7), 122));
		g.drawRect(x - 3, y - 12, s.length() * 7, 16);
		g.setColor(Color.BLACK);
		g.drawString(s, x + 1, y + 1);
		g.setColor(Color.WHITE);
		g.drawString(s, x, y);
	}

	@Override
	public void onPaint(Graphics gg) {
		long runTime = 0;
		runTime = System.currentTimeMillis() - scriptStartTIME;
		currentXp = this.getXp();
		int perHour = 0;
		if ((runTime / 1000) > 0) {
			perHour = (int) ((3600000.0 / (double) runTime) * ((this.currentXp - this.startXp)));
		}
		perHour = perHour / 1000;
		pointsGained = this.currentPoints - this.startPoints;
		int x = 560;
		int y = 350;
		Graphics2D g = (Graphics2D) gg;
		g.setColor(Color.YELLOW);
		rsDrawString("InBoat: " + this.inBoat(), x, 350, g);
		rsDrawString("InGame: " + this.inGame(), x, 350 + 15, g);
		rsDrawString("InCombat: " + inCombat, x, 350 + 30, g);
		rsDrawString("Status: " + this.status, x, 350 + 45, g);
		rsDrawString("Points Gained: " + this.pointsGained, x, 350 + 60, g);
		rsDrawString("Xp Gained: " + (this.currentXp - this.startXp) + " (" + perHour + "K/H)", x, 350 + 75, g);
		rsDrawString("Time running: " + getFormattedTime(runTime), x, y + 90, g);

	}

	public void clickSpecialAttack() {
		if (!GameTab.getOpen().equals(GameTab.TABS.COMBAT))
			GameTab.open(GameTab.TABS.COMBAT);
		sleep(300, 500);
		Mouse.moveBox(575, 415, 710, 428);
		sleep(450, 700);
		Mouse.click(0);
		sleep(300, 1600);
		if (random(0, 6) == 0)
			GameTab.open(GameTab.TABS.INVENTORY);
	}

	public int getSpecialAttack() {
		return (Game.getSettingsArray()[300] / 10);
	}

	public Image getImage(URL url) {
		try {
			return ImageIO.read(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	class DeathCapture implements Runnable {

		@Override
		public void run() {
			while (true) {
				if (Player.getRSPlayer().getHealthPercent() == 0)
					died = true;
				sleep(100);
			}
		}

	}

	class MouseControl implements Runnable {

		@Override
		public void run() {
			while (true) {

				if (random(0, 45) == 0) {
					if (getSpecialAttack() == 100) {
						clickSpecialAttack();
						sleep(300, 600);
					}
				}

				if (random(0, 35) == 0) {
					Mouse.pickupMouse();
				}

				if (!Game.isRunOn() && Game.getRunEnergy() > random(20, 55)) {
					Options.setRunEnabled(true);
				}

				if (random(0, 5) == 0) {
					Mouse.move(new Point(random(0, 764), random(0, 501)));
				}
				if (random(0, 15) == 0) {
					RSPlayer[] all = Players.getAll();
					if (all.length > 0) {
						RSPlayer p = all[random(0, all.length - 1)];
						if (p != null && p.isOnScreen() && p.getModel() != null) {
							Point po = p.getModel().getCentrePoint();
							if (po != null) {
								Mouse.move(po);
								sleep(200, 500);
								if (random(0, 2) == 0) {
									Mouse.click(2);
									sleep(50, 500);
								}
							}
						}
					}
				}
				if (random(0, 10) == 0)
					littleMouseMove();
				sleep(500, 5000);
			}
		}

	}

	class CameraControl implements Runnable {

		@Override
		public void run() {
			while (true) {
				if (target != null) {
					if (random(0, 1) == 0)
						target.adjustCameraTo();
				}
				if (random(0, 25) == 0) {
					RSCharacter it = Player.getRSPlayer().getInteractingCharacter();
					if (it != null) {
						RSTile loc = it.getPosition();
						if (loc != null) {
							loc.adjustCameraTo();
						}
					}
				}
				if (random(0, 10) == 0) {
					org.tribot.api2007.Camera.setCameraRotation(random(0, 360));
				}
				sleep(0, 5000);
			}
		}

	}

	class PlayerStatus implements Runnable {

		@Override
		public void run() {
			while (true) {
				inCombat = inCombatLoop();
			}
		}

	}

	private String getFormattedTime(long timeMillis) {
		long millis = timeMillis;
		final long seconds2 = millis / 1000;
		final long hours = millis / (1000 * 60 * 60);
		millis -= hours * 1000 * 60 * 60;
		final long minutes = millis / (1000 * 60);
		millis -= minutes * 1000 * 60;
		final long seconds = millis / 1000;
		String hoursString = "";
		String minutesString = "";
		String secondsString = seconds + "";
		String type = "seconds";

		if (minutes > 0) {
			minutesString = minutes + ":";
			type = "minutes";
		} else if (hours > 0 && seconds2 > 0) {
			minutesString = "0:";
		}
		if (hours > 0) {
			hoursString = hours + ":";
			type = "hours";
		}
		if (minutes < 10 && type != "seconds") {
			minutesString = "0" + minutesString;
		}
		if (hours < 10 && type == "hours") {
			hoursString = "0" + hoursString;
		}
		if (seconds < 10 && type != "seconds") {
			secondsString = "0" + secondsString;
		}

		return hoursString + minutesString + secondsString;
	}

}
