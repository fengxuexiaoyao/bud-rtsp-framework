
import com.buildud.config.RtspConfig;
import com.buildud.tools.ScreenShotUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@ComponentScan("com.buildud")
@EnableConfigurationProperties
public class BudNettyServerApplication{

	public static void main(String[] args) {
		initWidthAndHeigth();
		new SpringApplicationBuilder(BudNettyServerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	private static void initWidthAndHeigth(){
		int[] wh = ScreenShotUtils.getScreenWidthAndHeight();
		RtspConfig.screenWidth = wh[0];
		RtspConfig.screenHeigth = wh[1];
	}

}
