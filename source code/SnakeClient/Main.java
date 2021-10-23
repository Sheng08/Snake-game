import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.*;
 
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main extends JFrame{
	/**
	 * Create by ShengPo
	 */
	private static final long serialVersionUID = 1L;
	public static GamePanel p1 ;//創建遊戲面板
	public static InformationPanel p2 ;//創建資訊面板
	public static ServerListener p3;
	Socket socket;
	private ExecutorService exec= null;
	public static Snake snakeSelf;

	public Main(){//配置框架的佈局
		setLayout(new BorderLayout());
		

		try {
			socket  = new Socket("127.0.0.1", 54321);
			p1 = new GamePanel(socket);
			p2 = new InformationPanel(socket);
			p3 = new ServerListener(socket);
			snakeSelf = new Snake();
			add(p1,BorderLayout.CENTER);
			add(p2,BorderLayout.EAST);
			exec = Executors.newCachedThreadPool();
			
			exec.execute(p1);
			// Thread.sleep(1000);
			exec.execute(p2);
			// Thread.sleep(1000);
			exec.execute(p3);//要發送初始位置 所以要先開始 蛇頭初始都一樣 或在寫隨機-> 不行 要先創建版面再去更新聆聽初始至 不然p3會找不到版面
		}catch (Exception e) {
			System.out.println(e);
		}
	}
 
	public static void main(String[] args){
		JFrame frame = new Main();//新建框架
 
		//配置框架
		frame.setTitle("貪吃蛇");
		frame.setSize(1100, 800);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
 
	}
}
 
class GamePanel extends JPanel implements Runnable{
	public static final int PER_UNIT_LENGTH = 20;//單位長度
	public static final int MULTIPLE = 15;//倍數
	public static final int HALF_SIDE = MULTIPLE * PER_UNIT_LENGTH;//遊戲邊框的一半長 = 倍數 * 單位長度
	private boolean isStarted = false;//判斷是否開始
	private boolean isPaused = false;//判斷是否暫停
	public int score = 0;//遊戲分數
	private static int information = 0;//傳遞遊戲資訊
	static String HighScore = "";
	static String  No_self = "";
	public Snakes snakes = new Snakes();
	public Dot dessert = new Dot();//新建一個點心
	public int xCentre;
	public int yCentre;
	Socket Gsocket = null;
	Random rand = new Random();
	float r = rand.nextFloat();
	float g = rand.nextFloat();
	float b = rand.nextFloat();
	Color randomColor = new Color(r, g, b);
	private BufferedImage image;

	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
	  
		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
	  
		return dimg;
	  }
	  
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		
		URL resource = getClass().getResource("apple.png");
        try {
            image = ImageIO.read(resource);
			image = resize(image, 20, 20);
        } catch (IOException e) {
            e.printStackTrace();
        }

		//獲取中點座標
		xCentre = getWidth() / 2;
		yCentre = getHeight() / 2;

		g.drawRect(xCentre - HALF_SIDE, yCentre - HALF_SIDE, HALF_SIDE * 2, HALF_SIDE * 2);

		Snake snakeValue;
		for  (String snakeKey : snakes.getSnakes().keySet()) {  
            snakeValue = snakes.getSnakes().get(snakeKey);

			//繪畫蛇身
			g.setColor(Color.MAGENTA);
			for(int i = 0;i < snakeValue.getBody().size();i++){
				g.fillOval(snakeValue.getBody().get(i).getX(), snakeValue.getBody().get(i).getY(), PER_UNIT_LENGTH, PER_UNIT_LENGTH);
			}

			if (snakeKey.equals(ServerListener.Self_player)){
				g.setColor(Color.ORANGE);
				g.fillOval(snakeValue.getHead().getX(), snakeValue.getHead().getY(), PER_UNIT_LENGTH, PER_UNIT_LENGTH);
			}else{
				//繪畫蛇頭 //自動隨機玩家顏色 自己顏色固定 廣播??
				g.setColor(randomColor);
				g.fillOval(snakeValue.getHead().getX(), snakeValue.getHead().getY(), PER_UNIT_LENGTH, PER_UNIT_LENGTH);
			}

			
		}

		// g.setColor(Color.DARK_GRAY);
		// g.drawOval(dessert.getX(), dessert.getY(), PER_UNIT_LENGTH, PER_UNIT_LENGTH);
		g.drawImage(image, dessert.getX(), dessert.getY(), this);
		
		//如果遊戲結束，則追加繪畫GAME OVER
		if(isCrushed()){
			g.setColor(Color.BLACK);
			FontMetrics fm = g.getFontMetrics();
			int stringWidth = fm.stringWidth("GAME OVER");
			int stringAscent = fm.getAscent();
			int xCoordinate = xCentre - stringWidth / 2;
			int yCoordinate = yCentre - stringAscent / 2;
			g.drawString("GAMEOVER", xCoordinate, yCoordinate);
			try {
				PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
				pw.println("GAMEOVER:"+ServerListener.Self_player);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
 
	public GamePanel(Socket socket){
		this.Gsocket = socket;
		//配置面板屬性
		setFocusable(true);
		setFont(new Font("Californian FB", Font.BOLD, 80));
 
		//註冊鍵盤監聽器
		addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				int direction =  Main.snakeSelf.getDirection();
				switch(e.getKeyCode()){
					case KeyEvent.VK_UP:
						if(isStarted && !isPaused && !isCrushed()){ //優化isCrushed() 不要遍歷全部蛇
							if(direction != Snake.DIRECTION_UP && direction != Snake.DIRECTION_DOWN){
								Main.snakeSelf.setDirection(Snake.DIRECTION_UP);
								// changeSnakeLocation();
								try {
									PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
									pw.println("Direction:UP");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
						break;
					case KeyEvent.VK_DOWN:
						if(isStarted && !isPaused && !isCrushed()){
							if(direction != Snake.DIRECTION_UP && direction != Snake.DIRECTION_DOWN){
								Main.snakeSelf.setDirection(Snake.DIRECTION_DOWN);
								// changeSnakeLocation();
								try {
									PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
									pw.println("Direction:DOWN");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
						break;
					case KeyEvent.VK_LEFT:
						if(isStarted && !isPaused && !isCrushed()){
							if(direction != Snake.DIRECTION_LEFT && direction != Snake.DIRECTION_RIGHT){
								Main.snakeSelf.setDirection(Snake.DIRECTION_LEFT);
								// changeSnakeLocation();
								try {
									PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
									pw.println("Direction:LEFT");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
						break;
					case KeyEvent.VK_RIGHT:
						if(isStarted && !isPaused && !isCrushed()){
							if(direction != Snake.DIRECTION_LEFT && direction != Snake.DIRECTION_RIGHT){
								Main.snakeSelf.setDirection(Snake.DIRECTION_RIGHT);
								// changeSnakeLocation();
								try {
									PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
									pw.println("Direction:RIGHT");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
						break;
					case KeyEvent.VK_ENTER:
						if(isCrushed()){//如果遊戲結束，則重置資料重新開始遊戲 //如何更新單一隻蛇 或全部蛇
							// 重新連線時怎麼重新抓在server的蛇 server有心人連線全部重新廣播在場玩家 並只設定自己初始位置速度
							// 調整速度 全部 還是自己就好 速度調整原理 是加快方格? 其人不加速 遊戲特色

							Main.snakeSelf.setDirection(Snake.DIRECTION_DOWN);
							Main.snakeSelf.setSpeed(Snake.SPEED_3);
							Main.snakeSelf.setBody(new LinkedList<Dot>());
 
							isStarted = true;
							isPaused = false;
							iscrushed = false;
							score = 0;
							information = 0;
							ServerListener.flag2 = false;
							try {
								PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
								pw.println("GAMESTART:"+ServerListener.Self_player);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							System.out.println("FIRST");
							repaint();
						}
						else{
							isStarted = true;
							try {
								PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
								pw.println("FIRST:"+ServerListener.Self_player);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							System.out.println("FIRST");
						}
						break;
					case KeyEvent.VK_SPACE:
						if(isStarted && !isCrushed()){
							isPaused = !isPaused;
							try {
								PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
								pw.println("STOP:"+ServerListener.Self_player);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							System.out.println("STooooooooooop");
						}
							
						break;
					case KeyEvent.VK_F1: 
						try {
							PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
							pw.println("Speed:"+ServerListener.Self_player+":"+Snake.SPEED_1);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					case KeyEvent.VK_F2: 
						try {
							PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
							pw.println("Speed:"+ServerListener.Self_player+":"+Snake.SPEED_2);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					case KeyEvent.VK_F3: 
						try {
							PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
							pw.println("Speed:"+ServerListener.Self_player+":"+Snake.SPEED_3);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					case KeyEvent.VK_F4: 
						try {
							PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
							pw.println("Speed:"+ServerListener.Self_player+":"+Snake.SPEED_4);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					case KeyEvent.VK_F5: 
						// Main.snakeSelf.setSpeed(Snake.SPEED_5);
						try {
							PrintWriter pw = new PrintWriter(Gsocket.getOutputStream(), true);
							pw.println("Speed:"+ServerListener.Self_player+":"+Snake.SPEED_5);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					default:
				}
			}
		});
	}

	@Override
	public void run(){//控制蛇自動前進
	}

	static boolean eggChange= false;
	public   void changeSnakeLocation(){//改變蛇的位置資訊並重畫
		PrintWriter pw;
		// int xNow;int yNow;
		Snake snakeValue;
		if (eggChange==false){
			for  (String snakeKey : snakes.getSnakes().keySet()) {  
				snakeValue = snakes.getSnakes().get(snakeKey);

				if(isEncountered(snakeValue)){
					// score++;//由Server管
					// snake.getBody().addFirst(new Dot(xPrevious, yPrevious));
					snakeValue.getBody().addFirst(new Dot(snakeValue.getHead_Pre_X(), snakeValue.getHead_Pre_Y()));
					if (snakeKey.equals(ServerListener.Self_player)){
					try {
						pw = new PrintWriter(Gsocket.getOutputStream(), true);
						// player = setPlayers.getPlayers().get("1");
						pw.println("EGG:EAT:"+snakeKey);
					} catch (IOException e) {
						e.printStackTrace();
					}
					}
					snakeValue.setHead_Pre_X(snakeValue.getHead().getX());
					snakeValue.setHead_Pre_Y(snakeValue.getHead().getY());
				}
				else{
					if (ServerListener.isMe == true && snakeKey.equals(ServerListener.Self_player)){
						snakeValue.getBody().addFirst(new Dot(snakeValue.getHead_Pre_X(), snakeValue.getHead_Pre_Y()));
						snakeValue.getBody().removeLast();
						snakeValue.setHead_Pre_X(snakeValue.getHead().getX());
						snakeValue.setHead_Pre_Y(snakeValue.getHead().getY());
						ServerListener.isMe = false;
	
					}else if(!snakeKey.equals(ServerListener.Self_player)){
						snakeValue.getBody().addFirst(new Dot(snakeValue.getHead_Pre_X(), snakeValue.getHead_Pre_Y()));
						snakeValue.getBody().removeLast();
						snakeValue.setHead_Pre_X(snakeValue.getHead().getX());
						snakeValue.setHead_Pre_Y(snakeValue.getHead().getY());
					}
					
					if (snakeKey.equals(ServerListener.Self_player) && isPaused != true){
				
						boolean isCrushedByItself = false;
						for(int i = snakeValue.getBody().size()-1; i >=1  ;i--){
							if(snakeValue.getHead().getX() ==  snakeValue.getBody().get(i).getX() 
							&&  snakeValue.getHead().getY() ==  snakeValue.getBody().get(i).getY() && !isCrushedByItself){
								isCrushedByItself = true;
								iscrushed = true;
							}
								
						}
						if(isCrushedByItself){
							System.out.println("CrushedByItself");
							information = 2;
						}
					}
					
				}
				

			}
		}else{
			eggChange = !eggChange;
		}

		//重畫並獲取焦點
		repaint();
		requestFocus();
	}
 
	public boolean isEncountered(Snake snake){//判斷是否吃到點心
		if(snake.getHead().getX() == dessert.getX() 
		&& snake.getHead().getY() == dessert.getY()){
			System.out.println("EEEEEEEAT");
			return true;
		}
		else{
			return false;
		}
	}
	boolean iscrushed = false;
	public boolean isCrushed(){//判斷遊戲是否結束 server結束少人
		//先判斷是否碰觸邊框
		
		boolean isCrushedByBorder =  Main.snakeSelf.getHead().getX() >= getWidth() / 2 + HALF_SIDE  
		||  Main.snakeSelf.getHead().getX() < getWidth() / 2 - HALF_SIDE 
		||  Main.snakeSelf.getHead().getY() >= getHeight() / 2 + HALF_SIDE 
		||  Main.snakeSelf.getHead().getY() < getHeight() / 2 - HALF_SIDE;
		if(isCrushedByBorder){
			information = 1;
			iscrushed = true;
			// return true;
		}
		
		return iscrushed;
	}
 
	public int getScore(){
		return score;
	}
 
	public static int getInformation(){
		return information;
	}

	public Dot getDessert(){
		return dessert;
	}
}
 
class InformationPanel extends JPanel implements Runnable{
	
	private Box box = Box.createVerticalBox();//創建一個垂直盒子容器
	private JLabel[] help = new JLabel[5];//顯示?明資訊
	private JLabel score = new JLabel("自己分數：");//顯示分數
	private JLabel highScore = new JLabel("最高分玩家：");
	private JLabel No = new JLabel("你的名次：");
	private JLabel you = new JLabel("你是玩家：");

	private JLabel show = new JLabel();//顯示資訊
	
	public InformationPanel(Socket socket){
		
		//初始化陣列
		for(int i = 0;i < help.length;i++)
			help[i] = new JLabel();
 
		//配置字體
		Font font1 = new Font("DialogInput", Font.BOLD, 20);
		Font font2 = new Font("DialogInput", Font.BOLD + Font.ITALIC, 25);
		for(int i = 0;i < help.length;i++)
			help[i].setFont(font1);
		score.setFont(font2);
		score.setForeground(Color.BLUE);
		highScore.setFont(font2);
		highScore.setForeground(Color.GREEN);
		No.setFont(font2);
		No.setForeground(Color.GREEN);
		show.setFont(font2);
		show.setForeground(Color.RED);
		you.setFont(font2);
		you.setForeground(Color.ORANGE);
 
		//配置?明資訊
		help[0].setText("回車鍵開始遊戲");
		help[1].setText("方向鍵移動蛇");
		help[2].setText("空白鍵暫停遊戲");
		help[3].setText("按鍵F1-F5調節蛇速");
		help[4].setText("按Enter鍵可以重新開始遊戲");
 
		//配置資訊面板
		add(box);
		box.add(Box.createVerticalStrut(10));
		box.add(you);

		box.add(Box.createVerticalStrut(120));
		for(int i = 0;i < help.length;i++){
			box.add(help[i]);
			box.add(Box.createVerticalStrut(10));
		}
		box.add(Box.createVerticalStrut(45));
		box.add(score);
		box.add(Box.createVerticalStrut(45));
		box.add(highScore);
		box.add(Box.createVerticalStrut(45));
		box.add(No);
		box.add(Box.createVerticalStrut(45));
		box.add(show);		
	}
	String string1;
	String string2 = null;
	String string_A;
	String string_B;
	String string_you;
	@Override
	public void run(){//更新遊戲資訊
		while(true){
			string1 = "自己分數：" + Integer.toString(Main.p1.getScore());
			score.setText(string1);

			string_A = "最高分玩家：" +  GamePanel.HighScore +"號";
			highScore.setText(string_A);

			string_B = "你的名次：" +  GamePanel.No_self;
			No.setText(string_B);

			string_you = "你是玩家："+ServerListener.Self_player;
			you.setText(string_you);

			
			switch(GamePanel.getInformation()){
				case 0:string2 = "遊戲進行中";break;
				case 1:string2 = "你撞穿牆壁了！";break;
				case 2:string2 = "你吃到自己了！";break;
				default:
			}
			show.setText(string2);
		}
	}
}

class ServerListener implements Runnable{
	
	boolean flag = false;
	static boolean flag2 = false;
	Socket Ssocket = null;
	static String player = "";
	static String Self_player = "";
	static boolean isMe = false;


	public ServerListener(Socket socket){
		this.Ssocket = socket;
	}

	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(Ssocket.getInputStream()));
			String s1;
			while ((s1 = br.readLine()) != null) {
				System.out.println("cmd: "+s1);
				String control[] = s1.split(":");
				if(control[0].equals("Server")) {
					if(control[1].equals("EGG")) {

						System.out.println(control[1]+":"+control[2]+":"+control[3]);
						
						Main.p1.getDessert().setX(Integer.parseInt(control[2]));
						Main.p1.getDessert().setY(Integer.parseInt(control[3]));

						GamePanel.eggChange = true;
						Main.p1.changeSnakeLocation();
					}else if(control[1].equals("Player")) {
						
						if (flag==false){
							Self_player = control[2];
							// System.out.println(Self_player);
							Main.p1.snakes.getSnakes().put(Self_player,  Main.snakeSelf);//不加入自己的蛇 加入別人的蛇 自己用snakeSelf本運作
							flag = true;
							
						}else{
							if(!control[2].equals(Self_player)){
								player = control[2];
								Main.p1.snakes.getSnakes().put(player, new Snake());//不加入自己的蛇 加入別人的蛇 自己用snakeSelf本運作
								System.out.println(control[1]+":"+player);
							}
							
						}
						
						

						
					}else if(control[1].equals("Move")) {
						System.out.println(control[1]+":"+control[2]+":"+control[3]);
						System.out.println(control[1]+":"+control[2]+":"+control[4]);
						Main.p1.snakes.getSnakes().get(control[2]).getHead().setX(Integer.parseInt(control[3]));
						Main.p1.snakes.getSnakes().get(control[2]).getHead().setY(Integer.parseInt(control[4]));
						if (control[2].equals(Self_player)){
							isMe = true;
						}
						Main.p1.changeSnakeLocation();
						

					}else if (control[1].equals("Score")){
						System.out.println(control[1]+":"+control[2]+":"+control[3]);
						if(control[2].equals(Self_player)){
							Main.p1.score =  Integer.parseInt( control[3] );
						}
						
					}else if(control[1].equals("PlayersScorce")) {
						GamePanel.No_self = control[2];
						GamePanel.HighScore = control[3];
						
			
					}else if(control[1].equals("GAMEOVER")){
						Main.p1.snakes.getSnakes().get(control[2]).getBody().clear();
						System.out.println(control[1]+":"+control[2]+" GAMEOVER");
					}

				}
			}
			System.out.println("cmd: Out");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}

class SetPlayers{
	private Map<String, String> players = new LinkedHashMap<String,String>();	
	public SetPlayers(){
	}
	public Map<String, String> getPlayers(){
		return players;
	}

}

class Snakes{
	private Map<String, Snake> snakes = new LinkedHashMap<String, Snake>();	
	public Snakes(){
	}
	public Map<String, Snake> getSnakes(){
		return snakes;
	}

}

class Snake{//蛇類
	public static final int DIRECTION_UP = 1;
	public static final int DIRECTION_DOWN = 2;
	public static final int DIRECTION_LEFT = 3;
	public static final int DIRECTION_RIGHT = 4;
	public static final int SPEED_1 = 1000;
	public static final int SPEED_2 = 300;
	public static final int SPEED_3 = 200;
	public static final int SPEED_4 = 150;
	public static final int SPEED_5 = 100;
	private int direction = DIRECTION_DOWN;
	private int speed = SPEED_3;
	private Dot head = new Dot();
	private LinkedList<Dot> body = new LinkedList<Dot>();

	private int head_Pre_X = 0;private int head_Pre_Y = 0;
 
	public Snake(){
	}
 
	public Dot getHead(){
		return head;
	}
 
	public LinkedList<Dot> getBody(){
		return body;
	}
 
	public int getDirection(){
		return direction;
	}
 
	public int getSpeed(){
		return speed;
	}
 
	public void setBody(LinkedList<Dot> body){
		this.body = body;
	}
 
	public void setDirection(int direction){
		this.direction = direction;
	}
 
	public void setSpeed(int speed){
		this.speed = speed;
	}


	public int getHead_Pre_X(){
		return head_Pre_X;
	}
	public int getHead_Pre_Y(){
		return head_Pre_Y;
	}
	public void setHead_Pre_X(int head_Pre_X){
		this.head_Pre_X = head_Pre_X;
	}
	public void setHead_Pre_Y(int head_Pre_Y){
		this.head_Pre_Y = head_Pre_Y;
	}

}
 
class Dot{//點類
	private int x = 0;
	private int y = 0;
 
	public Dot(){
	}
 
	public Dot(int x, int y){
		this.x = x;
		this.y = y;
	}
 
	public int getX(){
		return x;
	}
 
	public int getY(){
		return y;
	}
 
	public void setX(int x){
		this.x = x;
	}
 
	public void setY(int y){
		this.y = y;
	}
}

