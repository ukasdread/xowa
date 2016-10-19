/*
XOWA: the XOWA Offline Wiki Application
Copyright (C) 2012 gnosygnu@gmail.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package gplx.xowa.guis.views; import gplx.*; import gplx.xowa.*; import gplx.xowa.guis.*;
import gplx.core.threads.*; import gplx.core.envs.*;
public class Load_page_wkr implements Gfo_thread_wkr {
	private final    Xog_tab_itm tab;
	public Load_page_wkr(Xog_tab_itm tab, Xowe_wiki wiki, Xoa_url url, Xoa_ttl ttl) {this.tab = tab; this.wiki = wiki; this.url = url; this.ttl = ttl;}
	public String				Thread__name()		{return "xowa.load_page_wkr";}
	public boolean				Thread__resume()	{return false;}
	public Xowe_wiki			Wiki()				{return wiki;}			private final    Xowe_wiki wiki;
	public Xoa_url				Url()				{return url;}			private final    Xoa_url url;
	public Xoa_ttl				Ttl()				{return ttl;}			private final    Xoa_ttl ttl;
	public Xoae_page			Page()				{return page;}			private Xoae_page page;
	public Exception		Exec_err()			{return exec_err;}		private Exception exec_err;
	public void Thread__exec() {
		try {
			Running_(true);

			// wait_for_popups; free mem check;
			this.page = wiki.Page_mgr().Load_page(url, ttl, tab);

			// launch thread to show page
			Gfo_invk_.Invk_by_val(tab.Cmd_sync(), Xog_tab_itm.Invk_show_url_loaded_swt, this);
		}
		catch (Exception e) {
			this.exec_err = e;
			Gfo_invk_.Invk_by_val(tab.Cmd_sync(), Xog_tab_itm.Invk_show_url_failed_swt, this);
		}
		finally {
			Running_(false);
		}
	}
	private static final    Object thread_lock = new Object(); private static boolean running = false;
	public static boolean Running() {
		boolean rv = false;
		synchronized (thread_lock) {
			rv = running;
		}
		return rv;
	}
	private static void Running_(boolean v) {
		synchronized (thread_lock) {
			running = v;
		}
	}
}