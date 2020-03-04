package launcher.managers;

import launcher.CaptchaWorkaroundForm;
import launcher.LoginForm;
import launcher.MainForm;
import launcher.TwoFactorForm;
import launcher.objects.Request;
import launcher.utils.Utils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class AuthenticationManager {
	private static String ARKOSELABS_PUBLIC_KEY = "37D033EB-6489-3763-2AE1-A228C04103F5";

	private static String REPUTATION = "https://www.unrealengine.com/id/api/reputation";
	private static String CSRF = "https://www.unrealengine.com/id/api/csrf";
	private static String CAPTCHA = "https://epic-games-api.arkoselabs.com/fc/gt2/public_key/" + ARKOSELABS_PUBLIC_KEY;
	private static String LOGIN = "https://www.unrealengine.com/id/api/login";
	private static String EXCHANGE = "https://www.epicgames.com/id/api/exchange";

	private static String OAUTH_TOKEN = "https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token";
	private static String OAUTH_EXCHANGE = "https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/exchange";
	private static String OAUTH_VERIFY = "https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/verify";

	private static String CODE = "MzRhMDJjZjhmNDQxNGUyOWIxNTkyMTg3NmRhMzZmOWE6ZGFhZmJjY2M3Mzc3NDUwMzlkZmZlNTNkOTRmYzc2Y2Y=";

	private String TOKEN = "";

	private int _stepsCount;
	private int _currentStep;

	private String _twoFactorMethod;

	public AuthenticationManager() {
		_stepsCount = 10;
		_currentStep = 0;
	}

	public void initialize() {
		SessionManager.getInstance().loadSession();
		LoginForm.getInstance();
		if (SessionManager.getInstance().getSession().requiresFullLogin()) {
			LoginForm.getInstance().allowActions();
			return;
		}
		boolean refreshSession = SessionManager.getInstance().getSession().isExpired();
		boolean refreshOAuth = SessionManager.getInstance().getUser().oAuthRefreshTokenExpired() || SessionManager.getInstance().getUser().oAuthAccessTokenExpired();
		if (refreshSession || refreshOAuth) {
			try {
				_stepsCount = 1 + (refreshOAuth ? 4 : 0) + (refreshSession ? 3 : 0);
				retrieveCSRF();
				if (refreshOAuth) {
					exchangeRequest();
					oAuthRequest();
					oAuthExchange();
					oAuthFinalRequest();
				}
				if (refreshSession)
					finishLogin();
				SessionManager.getInstance().saveSession();
				LoginForm.getInstance().setVisible(false);
				MainForm.getInstance().setUsernamePane(SessionManager.getInstance().getUser().getDisplayName()).initialize();
			} catch (RuntimeException re) {
				LoginForm.getInstance().loginError(re.getMessage());
				LoginForm.getInstance().allowActions();
			}
			return;
		}
		_stepsCount = 1;
		retrieveCSRF();
		LoginForm.getInstance().setVisible(false);
		MainForm.getInstance().setUsernamePane(SessionManager.getInstance().getUser().getDisplayName()).initialize();
	}

	private void successfullStep() {
		_currentStep++;
		LoginForm.getInstance().increaseProgressBarValue(_currentStep * 100 / _stepsCount);
	}

	public void doLogin() {
		_currentStep = 0;
		_stepsCount = 10;
		LoginForm.getInstance().clearProgress();
		try {
			_twoFactorMethod = null;
			SessionManager.getInstance().getSession().clearSession();
			captchaValidationRequest();
			loginRequest();
			if (_twoFactorMethod == null) {
				exchangeRequest();
				oAuthRequest();
				oAuthExchange();
				oAuthFinalRequest();
				finishLogin();
				SessionManager.getInstance().saveSession();
				LoginForm.getInstance().setVisible(false);
				MainForm.getInstance().setUsernamePane(SessionManager.getInstance().getUser().getDisplayName()).initialize();
			}
			else
				_stepsCount += 2;
		} catch (RuntimeException re) {
			LoginForm.getInstance().loginError(re.getMessage());
			LoginForm.getInstance().allowActions();
		}
	}

	private void captchaValidationRequest() throws RuntimeException {
		try {
			Request request = new Request(REPUTATION);
			if (!request.execute(200)) {
				throw new RuntimeException("Error: #1001");
			}
			JSONObject root = new JSONObject(request.getContent());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			if (root.has("verdict")) {
				String verdict = root.getString("verdict");
				if (!verdict.equalsIgnoreCase("allow")) {
					JSONObject object = root.getJSONObject("arkose_data");
					retrieveCatpchaInformation(object.getString("blob"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error: #1002");
		}
		successfullStep();
	}

	private void retrieveCatpchaInformation(String data) {
		String bda = SessionManager.getInstance().getSession().getBDA();
		if (bda == null)
			return;
		Random rnd = new Random();
		try {
			Request request = new Request(CAPTCHA).postRequest();
			request.assignInput("public_key", ARKOSELABS_PUBLIC_KEY);
			request.assignInput("bda", bda);
			request.assignInput("site", "https://epic-games-api.arkoselabs.com");
			request.assignInput("userbrowser", SessionManager.getInstance().getSession().getUserBrowser());
			request.assignInput("simulate_rate_limit", "0");
			request.assignInput("simulated", "0");
			request.assignInput("language", "en-US");
			request.assignInput("rnd", rnd.nextDouble());
			request.assignInput("data[blob]", data);
			request.removeUserAgent();
			if (!request.execute(200)) {
				throw new RuntimeException("Error: #1003");
			}
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("token")) {
				throw new RuntimeException("Error: #1004");
			}
			TOKEN = root.getString("token");
		} catch (IOException e) {
			throw new RuntimeException("Error: #1005");
		}
		successfullStep();
	}

	private void retrieveCSRF() {
		try {
			Request request = new Request(CSRF);
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignHeader("X-Epic-Event-Action", "login");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignHeader("Referer", "https://www.unrealengine.com/id/login");
			request.assignHeader("Host", "www.unrealengine.com");
			request.assignHeader("Accept", "application/json, text/plain, */*");
			if (!request.execute(204)) {
				throw new RuntimeException("Error: #1006");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
		} catch (IOException e) {
			throw new RuntimeException("Error: #1007");
		}
		successfullStep();
	}

	private void loginRequest() {
		retrieveCSRF();
		try {
			Request request = new Request(LOGIN);
			request.assignCookies(SessionManager.getInstance().getSession()).assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignCookie("EPIC_COUNTRY", "LT");
			request.assignCookie("EPIC_LOCALE_COOKIE", "en-US");
			request.assignHeader("X-Epic-Event-Action", "login");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignHeader("Referer", "https://www.unrealengine.com/id/login");
			request.assignHeader("Host", "www.unrealengine.com");
			request.assignHeader("Accept", "application/json, text/plain, */*");
			request.assignInput("email", SessionManager.getInstance().getUser().getEmail()).assignInput("password", SessionManager.getInstance().getUser().getPassword()).assignInput("rememberMe", false).assignInput("captcha", TOKEN);
			request.postRequest().jsonContentType();
			if (!request.execute(200)) {
				if (request.getResponseCode() == 431) {
					SessionManager.getInstance().getSession().setCookies(request.getCookies());
					performTwoFactor(request.getContent());
					throw new RuntimeException("Two factor authentication required.");
				}
				else {
					System.out.println(request.getResponseCode());
					System.out.println(request.getContent());
					JSONObject object = new JSONObject(request.getContent());
					if (object.has("message")) {
						if (object.has("errorCode")) {
							String errorCode = object.getString("errorCode");
							if (errorCode.equals("errors.com.epicgames.accountportal.captcha_invalid")) {
								CaptchaWorkaroundForm.getInstance().start();
								throw new RuntimeException("Captcha data necessary to finish authentication.");
							}
						}
						throw new RuntimeException(object.getString("message"));
					}
				}
				throw new RuntimeException("Error: #1008");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
		} catch (IOException e) {
			throw new RuntimeException("Error: #1009");
		}
		successfullStep();
	}

	private void exchangeRequest() {
		try {
			Request request = new Request(EXCHANGE);
			request.assignCookies(SessionManager.getInstance().getSession()).assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Error: #1010");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("code")) {
				System.out.println(request);
				throw new RuntimeException("Error: #1011");
			}
			SessionManager.getInstance().getUser().setExchangeCode(root.getString("code"));
		} catch (IOException e) {
			throw new RuntimeException("Error: #1012");
		}
		successfullStep();
	}

	private void oAuthRequest() {
		try {
			Request request = new Request(OAUTH_TOKEN);
			request.assignInput("grant_type", "exchange_code");
			request.assignInput("exchange_code", SessionManager.getInstance().getUser().getExchangeCode());
			request.assignInput("includePerms", true);
			request.assignInput("token_type", "eg1");
			request.assignHeader("Authorization", "basic " + CODE);
			request.assignHeader("X-Epic-Device-ID", generateDeviceId());
			request.assignCookies(SessionManager.getInstance().getSession()).assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.postRequest();
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Error: #1013");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("access_token")) {
				throw new RuntimeException("Error: #1014");
			}
			System.out.println(root);
			SessionManager.getInstance().getUser().setOAuthAccessToken(root.getString("access_token"));
		} catch (IOException e) {
			throw new RuntimeException("Error: #1015");
		}
		successfullStep();
	}

	private void oAuthExchange() {
		try {
			Request request = new Request(OAUTH_EXCHANGE);
			request.assignCookies(SessionManager.getInstance().getSession()).assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignHeader("Authorization", "bearer " + SessionManager.getInstance().getUser().getOAuthAccessToken());
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Error: #1016");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("code")) {
				System.out.println(request);
				throw new RuntimeException("Error: #1017");
			}
			SessionManager.getInstance().getUser().setOAuthExchangeCode(root.getString("code"));
		} catch (IOException e) {
			throw new RuntimeException("Error: #1018");
		}
		successfullStep();
	}

	private void oAuthFinalRequest() {
		try {
			Request request = new Request(OAUTH_TOKEN);
			request.assignInput("grant_type", "exchange_code");
			request.assignInput("exchange_code", SessionManager.getInstance().getUser().getOAuthExchangeCode());
			request.assignInput("includePerms", false);
			request.assignInput("token_type", "eg1");
			request.assignHeader("Authorization", "basic " + CODE);
			request.assignHeader("X-Epic-Device-ID", generateDeviceId());
			request.assignCookies(SessionManager.getInstance().getSession()).assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.postRequest();
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Error: #1019");
			}
			SessionManager.getInstance().getSession().setXSRF(request.getXSRF());
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("access_token")) {
				System.out.println(request);
				throw new RuntimeException("Error: #1020");
			}
			System.out.println(root.getString("access_token"));
			SessionManager.getInstance().getUser().setOAuthAccessToken(root.getString("access_token"));
			SessionManager.getInstance().getUser().setOAuthAccessTokenExpirationDate(root.getString("expires_at"));
			SessionManager.getInstance().getUser().setOAuthRefreshToken(root.getString("refresh_token"));
			SessionManager.getInstance().getUser().setOAuthRefreshTokenExpirationDate(root.getString("refresh_expires_at"));
			SessionManager.getInstance().getUser().setAccountId(root.getString("account_id"));
			SessionManager.getInstance().getUser().setClientId(root.getString("client_id"));
			SessionManager.getInstance().getUser().setAppId(root.getString("in_app_id"));
			SessionManager.getInstance().getUser().setDeviceId(root.getString("device_id"));
		} catch (IOException e) {
			throw new RuntimeException("Error: #1023");
		}
		successfullStep();
	}

	private void finishLogin() {
		String sid = "";
		try {
			Request request = new Request("https://www.unrealengine.com/id/api/redirect?redirectUrl=https%3A%2F%2Fwww.unrealengine.com%2Fen-US%2F&clientId=932e595bedb643d9ba56d3e1089a5c4b");
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignHeader("X-Epic-Event-Action", "login");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignHeader("Referer", "https://www.unrealengine.com/id/login");
			request.assignHeader("Host", "www.unrealengine.com");
			request.assignHeader("Accept", "application/json, text/plain, */*");
			if (!request.execute(200)) {
				throw new RuntimeException("Error: #1024");
			}
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("sid")) {
				throw new RuntimeException("Error: #1025");
			}
			sid = root.getString("sid");
			System.out.println(request);
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
		} catch (IOException e) {
			throw new RuntimeException("Error: #1026");
		}
		successfullStep();
		try {
			Request request = new Request("https://www.epicgames.com/id/api/set-sid?sid=" + sid);
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignHeader("X-Epic-Event-Action", "login");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignHeader("Referer", "https://www.unrealengine.com/id/login");
			request.assignHeader("Host", "www.unrealengine.com");
			request.assignHeader("Accept", "application/json, text/plain, */*");
			if (!request.execute(204)) {
				throw new RuntimeException("Error: #1027");
			}
			System.out.println(request);
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
		} catch (IOException e) {
			throw new RuntimeException("Error: #1028");
		}
		successfullStep();
		try {
			Request request = new Request("https://www.unrealengine.com/api/v2/my/account");
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignHeader("X-Epic-Event-Action", "login");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignHeader("Referer", "https://www.unrealengine.com/id/login");
			request.assignHeader("Host", "www.unrealengine.com");
			request.assignHeader("Accept", "application/json, text/plain, */*");
			if (!request.execute(200)) {
				throw new RuntimeException("Error: #1029");
			}
			System.out.println(request);
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("isLoggedIn")) {
				throw new RuntimeException("Error: #1030");
			}
			if (!root.getBoolean("isLoggedIn")) {
				throw new RuntimeException("Error: #1031");
			}
			SessionManager.getInstance().getUser().setDisplayName(root.getString("displayName"));
		} catch (IOException e) {
			throw new RuntimeException("Error: #1032");
		}
		successfullStep();
	}

	private void performTwoFactor(String content) {
		JSONObject root = new JSONObject(content);
		if (!root.has("errorCode"))
			throw new RuntimeException("Error: #1033");
		String errorCode = root.getString("errorCode");
		if (!errorCode.equalsIgnoreCase("errors.com.epicgames.common.two_factor_authentication.required"))
			throw new RuntimeException("Error: #1034");
		if (!root.has("metadata"))
			throw new RuntimeException("Error: #1035");
		JSONObject metadata = root.getJSONObject("metadata");
		if (!metadata.has("twoFactorMethod"))
			throw new RuntimeException("Error: #1036");
		_twoFactorMethod = metadata.getString("twoFactorMethod");

		LoginForm.getInstance().setErrorText("You have to fill two factor auth.");
		TwoFactorForm.getInstance().start();
	}

	public void confirmTwoFactorAuth(String code) {
		retrieveCSRF();

		try {
			Request request = new Request("https://www.unrealengine.com/id/api/login/mfa");
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignHeader("X-Epic-Event-Action", "mfa");
			request.assignHeader("X-Epic-Event-Category", "login");
			request.assignHeader("X-Epic-Strategy-Flags", "guardianEmailVerifyEnabled=false;guardianEmbeddedDocusignEnabled=true;guardianKwsFlowEnabled=false;minorPreRegisterEnabled=false;registerEmailPreVerifyEnabled=false");
			request.assignHeader("X-Requested-With", "XMLHttpRequest");
			request.assignInput("code", code).assignInput("method", _twoFactorMethod).assignInput("rememberDevice", false);
			request.jsonContentType().postRequest();
			if (!request.execute(200)) {
				System.out.println(request);
				TwoFactorForm.getInstance().start();
				TwoFactorForm.getInstance().showError("Invalid");
				return;
			}
			System.out.println(request);
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
		} catch (Exception e) {
			LoginForm.getInstance().loginError("Error: #1038");
			return;
		}
		try {
			successfullStep();
			exchangeRequest();
			oAuthRequest();
			oAuthExchange();
			oAuthFinalRequest();
			finishLogin();
			SessionManager.getInstance().saveSession();
			LoginForm.getInstance().setVisible(false);
			MainForm.getInstance().setUsernamePane(SessionManager.getInstance().getUser().getDisplayName()).initialize();
		} catch (RuntimeException re) {
			LoginForm.getInstance().loginError(re.getMessage());
			LoginForm.getInstance().allowActions();
		}
	}

	public static String generateDeviceId() {
		String deviceId = "";
		deviceId += Utils.randomString(8);
		deviceId += "-";
		deviceId += Utils.randomString(4);
		deviceId += "-";
		deviceId += Utils.randomString(4);
		deviceId += "-";
		deviceId += Utils.randomString(4);
		deviceId += "-";
		deviceId += Utils.randomString(12);
		return deviceId;
	}

	public static AuthenticationManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final AuthenticationManager _instance = new AuthenticationManager();
	}
}
