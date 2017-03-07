public synchronized Boolean connect(String accountName,String password){
		Hashtable<String,String> env = new Hashtable<String,String>();
		ctx = null;
		if("".equals(password.trim())){
			password = null;
		}
		env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, accountName);
		env.put(Context.SECURITY_CREDENTIALS, password);
		env.put(Context.PROVIDER_URL, "LDAP://10.68.100.211:389");
		try {
			ctx = new InitialDirContext(env);
			return true;
		} catch (AuthenticationException e) {
			System.out.println("��֤ʧ�ܣ�"+e.getMessage());
		}catch (NamingException e) {
			e.printStackTrace();
			System.out.println("��֤����");
		}
		return false;
	}