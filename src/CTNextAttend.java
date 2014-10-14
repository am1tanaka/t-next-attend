import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import java.io.File;


public class CTNextAttend {
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder builder = null;
	private static Node root = null;
	private static NodeList nodeList = null;
	
	public static void main(String[] args)
	{
		WebDriver driver = new FirefoxDriver();
		driver.get("https://next.tama.ac.jp/up/faces/login/Com00505A.jsp");
		String id="id";
		String pass="";

		// パラメータの読み込み
		id = getConfig(args[0],"id");
		pass = getConfig(args[0],"pass");
		
		// IDとパスワードを入力
		driver.findElement(By.id("form1:htmlUserId")).sendKeys(id);
		driver.findElement(By.id("form1:htmlPassword")).sendKeys(pass);
		driver.findElement(By.id("form1:login")).click();
		
		// 出欠登録へ
		JavascriptExecutor exe = (JavascriptExecutor)driver;
		exe.executeScript("window.clickSiteMenuItem(308,0);");
		
		// アラートを継続する
		Alert alt = driver.switchTo().alert();
		alt.accept();
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
