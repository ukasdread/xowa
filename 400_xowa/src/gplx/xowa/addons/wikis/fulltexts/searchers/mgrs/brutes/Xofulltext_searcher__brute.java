/*
XOWA: the XOWA Offline Wiki Application
Copyright (C) 2012-2017 gnosygnu@gmail.com

XOWA is licensed under the terms of the General Public License (GPL) Version 3,
or alternatively under the terms of the Apache License Version 2.0.

You may use XOWA according to either of these licenses as is most appropriate
for your project on a case-by-case basis.

The terms of each license can be found in the source code repository:

GPLv3 License: https://github.com/gnosygnu/xowa/blob/master/LICENSE-GPLv3.txt
Apache License: https://github.com/gnosygnu/xowa/blob/master/LICENSE-APACHE2.txt
*/
package gplx.xowa.addons.wikis.fulltexts.searchers.mgrs.brutes; import gplx.*; import gplx.xowa.*; import gplx.xowa.addons.*; import gplx.xowa.addons.wikis.*; import gplx.xowa.addons.wikis.fulltexts.*; import gplx.xowa.addons.wikis.fulltexts.searchers.*; import gplx.xowa.addons.wikis.fulltexts.searchers.mgrs.*;
import gplx.dbs.*;
import gplx.xowa.guis.cbks.*;
import gplx.xowa.addons.wikis.fulltexts.searchers.mgrs.uis.*;
import gplx.xowa.addons.wikis.fulltexts.searchers.mgrs.brutes.finders.*;
import gplx.xowa.addons.wikis.fulltexts.searchers.caches.*;
public class Xofulltext_searcher__brute implements Xofulltext_searcher {
	private final    Xofulltext_finder_mgr finder = new Xofulltext_finder_mgr();
	private final    Xofulltext_finder_cbk__eval cbk_eval = new Xofulltext_finder_cbk__eval();
	private final    Xofulltext_finder_cbk__highlight cbk_highlight = new Xofulltext_finder_cbk__highlight();
	public void Search(Xofulltext_searcher_ui ui, Xow_wiki wiki, Xofulltext_searcher_args args) {
		// get pages from db
		Db_conn page_conn = wiki.Data__core_mgr().Tbl__page().Conn();
		Db_rdr page_rdr = page_conn.Stmt_sql("SELECT * FROM page WHERE page_namespace IN (0) ORDER BY page_score DESC").Exec_select__rls_auto();

		// init finder
		finder.Init(args.query, args.case_match, args.auto_wildcard_bgn, args.auto_wildcard_end, Byte_ascii.Star, Byte_ascii.Dash);

		// loop
		byte[] wiki_domain = wiki.Domain_bry();
		int found = 0;
		int searched = 0;
		try {
			while (page_rdr.Move_next()) {
				// read data from reader
				int page_id = page_rdr.Read_int("page_id");
				int text_db_id = page_rdr.Read_int("page_text_db_id");
				byte[] text_mcase = wiki.Data__core_mgr().Dbs__get_by_id_or_fail(text_db_id).Tbl__text().Select(page_id);
				int ns_id = page_rdr.Read_int("page_namespace");
				byte[] ttl_bry = page_rdr.Read_bry_by_str("page_title");
				Xoa_ttl ttl = wiki.Ttl_parse(ns_id, ttl_bry);

				// eval query
				cbk_eval.Init(ttl.Full_db());
				finder.Match(text_mcase, 0, text_mcase.length, cbk_eval);
				searched++;

				// check if page matches query
				if (cbk_eval.found) {
					++found;

					// update pages found
					ui.Send_wiki_update(wiki_domain, found, searched);

					// do highlight
					if (found <= args.max_pages_per_wiki) {
						cbk_highlight.Init(ui, args.query_id, wiki, page_id, ttl.Full_db(), args.show_all_matches);
						ui.Send_page_add(new Xofulltext_searcher_page
							( args.query_id
							, String_.new_u8(wiki_domain)
							, page_id
							, String_.new_u8(ttl.Full_db())
							, args.expand_matches_section
							));
						finder.Match(text_mcase, 0, text_mcase.length, cbk_highlight);
					}
				}

				// update update pages found every 100 pages
				if (searched % 100 == 0) {
					ui.Send_wiki_update(wiki_domain, found, searched);
				}
			}
		}
		finally {
			page_rdr.Rls();
		}

		// update one last time for final searched
		ui.Send_wiki_update(wiki_domain, found, searched);
	}
}