import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


public class CTNextAttend {
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder builder = null;
	private static Node root = null;
	private static NodeList nodeList = null;
	private static WebDriver driver;
	private static JavascriptExecutor exe;
	private static ArrayList<String> entry = new ArrayList<String>();
	private static int iCheckingIndex = 0;
	
	public static void main(String[] args)
	{
		driver = new FirefoxDriver();
		driver.get("https://next.tama.ac.jp/up/faces/login/Com00505A.jsp");
		//driver.get("http://localhost/tama/キャンパスインフォメーション_sub.html");
		// 実行インスタンスを取得
		exe = (JavascriptExecutor)driver;

		// 履修者リストを読み込み
		loadEntryList(getConfig(args[0],"entrylist"));

		// ログインをして、次のページへ進む
		login1();

		// ウィンドウを切り替えて、ログインして講義ページへ
		login2();
		
		ArrayList<String> datelist = new ArrayList<String>();
		// CSVファイルを読み込む
		try {
			BufferedReader br = new BufferedReader(new FileReader(getConfig("","attendfile")));
			int linenum = 0;
			int LOAD_MAX = 1000;
			for (linenum=0; br.ready() && (linenum<LOAD_MAX) ; linenum++) {
				// 学籍番号まで飛ばす
				String line = br.readLine();
				if (linenum < 2) continue;

				// 分割
				String [] sepa = line.split(",");

				// 日付を取り出す
				if (linenum == 2) {
					for (int j=5 ; j<sepa.length ; j+=3)
					{
						datelist.add(sepa[j]);
						System.out.println(sepa[j]);
					}
					System.out.println("datenum="+datelist.size());
					continue;
				}
				
				// 行頭を飛ばす
				if (linenum < 5) continue;
				
				// void(subwnd_update('tanaka','21011332','1300022600','2014/09/24','4','-1'));
				// id
				// 学生学籍番号
				// classcode
				// 日付
				// 時限
				// コード(-1=未設定 / 0=欠席 / 1=出席 / 2=遅刻 / 3=例外 / 4=早退)
				
				// 出席登録開始
				if (sepa[0].length() == 0) {
					continue;
				}
				System.out.println("学籍番号="+sepa[0]);
				
				// 有効チェック
				Boolean isval = false;
				for (int j=0 ; j<entry.size();j++) 
				{
					if (entry.get(j).indexOf(sepa[0]) >= 0)
					{
						isval = true;
						break;
					}
				}
				
				if (!isval)
				{
					System.out.println("未登録");
					continue;
				}
				
				// カード番号があれば出席
				// 遅刻欄があればその値をそのまま設定(早退は遅刻扱いでよい)
				for (int j=5,dt=0 ; j<sepa.length ; j+=3,dt++)
				{
					int num = 0;
					// 時間が入っていれば出席
					System.out.print("["+j+"/"+sepa.length+"]("+sepa[j]+","+sepa[j+1]+")");
					if (sepa[j].length() > 1) {
						num = 1;

						// 出席者は引き続き遅刻のチェック。遅刻が0以外の時は、1を加える(遅刻が2のため)
						try {
							if (sepa[j+1].length() > 0) {
								num = Integer.parseInt(sepa[j+1])+1;
								if (num >4) {
									num = 4;
								}
							}
						}
						catch (Exception ee)
						{}
					}
					System.out.println("="+num);
					
					// 登録処理
					subwnd_update(
							getConfig("","id"),
							sepa[0],
							getConfig("","classcode"),
							datelist.get(dt),
							getConfig("","period"),
							num);
					// 2コマめチェック
					if (getConfig("","periodnum").compareTo("2") == 0)
					{
						// 遅刻は出席に変更
						if (num == 2) {
							num = 1;
						}
						subwnd_update(
								getConfig("","id"),
								sepa[0],
								getConfig("","classcode"),
								datelist.get(dt),
								""+(Integer.parseInt(getConfig("","period"))+1),
								num);
					}
					
					// TODO:動作テスト
					//break;
					
				}

				System.out.println("");
				// TODO:動作テスト
				//break;

			}
			
			br.close();
		}
		catch (Exception e) {
			System.out.println(e.toString());
			return;
		}
	}
	
	/**
	 * トップ画面からログインして、警告を続けるまで
	 */
	private static void login1()
	{
		// IDとパスワードを入力
		driver.findElement(By.id("form1:htmlUserId")).sendKeys(getConfig("","id"));
		driver.findElement(By.id("form1:htmlPassword")).sendKeys(getConfig("","pass"));
		driver.findElement(By.id("form1:login")).click();

		// 出欠登録へ
		exe.executeScript("window.clickSiteMenuItem(308,0);");
		
		// アラートを継続する
		Alert alt = driver.switchTo().alert();
		alt.accept();		
	}
	
	/*
	 * 2番目のログインを通して、講義を検索して出欠ページへ
	 */
	private static void login2()
	{
		int i;
		Object [] wins;

		for (i=0 ; i<10 ; i++)
		{
	    	wins = driver.getWindowHandles().toArray();
			if (wins.length > 1) {
		    	driver.switchTo().window(wins[1].toString());
			}
			
			try {
				driver.findElement(By.id("loginId")).sendKeys(getConfig("","id"));
				driver.findElement(By.id("password")).sendKeys(getConfig("","pass"));
				driver.findElement(By.id("doLogin")).click();
			}
			catch (Exception e){
				System.out.println(e.toString());
				continue;
			}
			break;
		}
		if (i == 10) {
			return;
		}
		
		// 講義検索
		exe.executeScript("goPage('/SyussekiCount/start.jsp?Jump=lecturelist');");
		exe.executeScript("void(go_search());");
		
		// 講義ページへ
		exe.executeScript("void(go_nextpage('"+getConfig("","classcode")+"'));");
	}
	
	/**
	 * 履修者リストの作成
	 * @param ファイル名
	 */
	private static void loadEntryList(String config) {
		entry.clear();

		// CSVファイルを読み込む
		try {
			BufferedReader br = new BufferedReader(new FileReader(config));
			int linenum = 0;
			for (linenum=0; br.ready() ; linenum++) {
				// 学籍番号まで飛ばす
				String line = br.readLine();
				if (linenum < 3) continue;

				// 分割
				String [] sepa = line.split(",");
				if (sepa[0].length() > 0) {
					entry.add(sepa[0]);
					System.out.println(sepa[0]);
				}
			}
		}
		catch (Exception e) {
			System.out.println("error:"+e.toString());
		}
	}

	/**
	 * 状態の設定
	 * @param id
	 * @param uid
	 * @param classcode
	 * @param dt
	 * @param period
	 * @param num
	 */
	private static void subwnd_update(String id,String uid,String classcode,String dt,String period,int num)
	{
		// 日付を作成。2けた
		String [] dtsepa = dt.split("/");
		String querydate = dtsepa[0]+"/"+("0"+dtsepa[1]).substring(dtsepa[1].length()-1)+"/"+("0"+dtsepa[2]).substring(dtsepa[2].length()-1);
		
		List<WebElement> atts = driver.findElements(By.tagName("a"));
		Boolean isClicked = false;
		for (int i=iCheckingIndex,loop=0 ; loop<atts.size() ; loop++,i++) {
			if (i >= atts.size()) {
				i = 0;
			}
			
			String [] href = atts.get(i).getAttribute("href").split(",");
			if (href.length < 5) {
				continue;
			}
			
			// 学籍番号と日付と時限が一致していたらクリック
			if (	(href[1].indexOf(uid) >= 0) 
				&&	(href[3].indexOf(querydate) >=0)
				&&	(href[4].indexOf(period) >= 0))
			{
				System.out.print("click "+atts.get(i).getAttribute("href"));
				iCheckingIndex = i+1;
				atts.get(i).click();
				System.out.println("-clicked");
				isClicked = true;
				break;
			}
		}

		// クリックしたので、次は選択肢
		if (isClicked) {
			// 時間待ち
			try {
				// フレームを変更
				driver.switchTo().frame("frmSub_content");
				//Thread.sleep(200);
				
				// 選択肢を調整
				List<WebElement> sstas = null;
				do {
					sstas = driver.findElements(By.name("sstatus"));
					if (sstas.size() == 0) {
						Thread.sleep(100);
					}
				}
				while (sstas.size() == 0);
				
				WebElement sstatus = driver.findElement(By.name("sstatus"));
				//System.out.println("sstatus="+sstatus.toString());
				Select sel = new Select(sstatus);
				// TODO:強制設定
				//num = 0;
				sel.selectByValue(""+num);
				
				//Thread.sleep(200);

				//exe.executeScript("window.go();");

				// 実行
				List<WebElement> btns = driver.findElements(By.tagName("img"));
				for (int j=0 ; j<btns.size() ; j++) {
					if (btns.get(j).getAttribute("src").indexOf("images/btn_change_f1.gif") != -1)
					{
						System.out.print("click get("+j+")");
						btns.get(j).click();
						System.out.println("-clicked");						
						break;
					}
				}
				
				//Thread.sleep(5000);
			} catch (Exception e) 
			{
				System.out.println("sleep switch :"+e.toString());
			}
			
			driver.switchTo().defaultContent();
		}
		else 
		{
			// 見つからなかったので、次のページへ
			
		}
	}

	
	/**
	 * コンフィグファイルから指定のタグの値を返す
	 * @param fname XMLファイルへのパス
	 * @param tag 取り出すタグ名
	 * @return 指定のタグの文字列
	 */
	private static String getConfig(String fname,String tag)
	{
		// XMLが読み込まれていない時は、まずは読み込む
		if (builder == null)
		{
			try {
				// ビルダーの生成
				builder = factory.newDocumentBuilder();
				
				// ファイルを読み込む
				File file = new File(fname);
				root = builder.parse(file);
				nodeList = root.getChildNodes().item(0).getChildNodes();
			} catch (Exception e)
			{
				System.out.println(e.toString());
				return "invalid file";
			}
			
		}

		// 検索
		for (int i=0 ; i<nodeList.getLength() ; i++) 
		{
			Node curr = nodeList.item(i);
			if (curr.getNodeType() != Node.ELEMENT_NODE) continue;
			if (curr.getNodeName().compareTo(tag) == 0) 
			{
				return curr.getFirstChild().getNodeValue();
			}
		}
		
		return "";
	}
}
