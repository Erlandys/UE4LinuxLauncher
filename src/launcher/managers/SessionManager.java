package launcher.managers;

import launcher.objects.User;
import launcher.objects.Session;

public class SessionManager {
	private User _user;
	private Session _session;

	SessionManager() {
		_user = new User();
		_session = new Session();
	}

	public User getUser() {
		return _user;
	}

	public Session getSession() {
		return _session;
	}

	public void saveSession() {
		_session.save();
		_user.save();
	}

	public void loadSession() {
		_session.load();
		_user.load();
	}

	public static SessionManager getInstance() {
		return SessionManager.SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final SessionManager _instance = new SessionManager();
	}
}
