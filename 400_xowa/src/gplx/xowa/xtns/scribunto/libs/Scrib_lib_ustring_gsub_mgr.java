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
package gplx.xowa.xtns.scribunto.libs; import gplx.*; import gplx.xowa.*; import gplx.xowa.xtns.*; import gplx.xowa.xtns.scribunto.*;
import gplx.langs.regxs.*;
import gplx.xowa.xtns.scribunto.procs.*;
class Scrib_lib_ustring_gsub_mgr {
	private final    Scrib_core core;
	private final    Scrib_regx_converter regx_converter;
	private byte[] repl_bry; private Hash_adp repl_hash; private Scrib_lua_proc repl_func;
	private int repl_count = 0;
	public Scrib_lib_ustring_gsub_mgr(Scrib_core core, Scrib_regx_converter regx_converter) {
		this.core = core;
		this.regx_converter = regx_converter;
	}
	public boolean Exec(Scrib_proc_args args, Scrib_proc_rslt rslt) {
		// get @text; NOTE: sometimes int; DATE:2013-11-06
		String text = args.Xstr_str_or_null(0);
		if (args.Len() == 2) return rslt.Init_obj(text); // if no @replace, return @text; PAGE:en.d:'orse; DATE:2013-10-13

		// get @pattern; NOTE: sometimes int; PAGE:en.d:λύω; DATE:2014-09-02
		String regx = args.Xstr_str_or_null(1);
		regx = regx_converter.patternToRegex(Bry_.new_u8(regx), Scrib_regx_converter.Anchor_pow);

		// get @repl
		Object repl_obj = args.Cast_obj_or_null(2);
		byte repl_tid = Identify_repl(repl_obj);

		// get @limit; reset repl_count
		int limit = args.Cast_int_or(3, -1);
		repl_count = 0;

		// do repl
		String repl = Exec_repl(repl_tid, text, regx, limit);
		return rslt.Init_many_objs(repl, repl_count);
	}
	private byte Identify_repl(Object repl_obj) {
		byte repl_tid = Repl_tid_null;
		// @repl can be String, int, table, func
		Class<?> repl_type = repl_obj.getClass();
		if		(Object_.Eq(repl_type, String_.Cls_ref_type)) {
			repl_tid = Repl_tid_string;
			repl_bry = Bry_.new_u8((String)repl_obj);
		}
		else if	(Object_.Eq(repl_type, Int_.Cls_ref_type)) {	// NOTE:@replace sometimes int; PAGE:en.d:λύω; DATE:2014-09-02
			repl_tid = Repl_tid_string;
			repl_bry = Bry_.new_u8(Int_.To_str(Int_.Cast(repl_obj)));
		}
		else if	(Object_.Eq(repl_type, Keyval[].class)) {
			repl_tid = Repl_tid_table;
			repl_hash = Hash_adp_.New();
			Keyval[] kvs = (Keyval[])repl_obj;
			int kvs_len = kvs.length;
			for (int i = 0; i < kvs_len; i++) {
				Keyval kv = kvs[i];
				repl_hash.Add(kv.Key(), Bry_.new_u8(kv.Val_to_str_or_empty()));
			}
		}
		else if	(Object_.Eq(repl_type, Scrib_lua_proc.class)) {
			repl_tid = Repl_tid_luacbk;
			repl_func = (Scrib_lua_proc)repl_obj;
		}
		else if	(Object_.Eq(repl_type, Double_.Cls_ref_type)) {	// NOTE:@replace sometimes double; PAGE:de.v:Wikivoyage:Wikidata/Test_Modul:Wikidata2; DATE:2016-04-21
			repl_tid = Repl_tid_string;
			repl_bry = Bry_.new_u8(Double_.To_str(Double_.cast(repl_obj)));
		}
		else
			throw Err_.new_unhandled(Type_.Name(repl_type));
		return repl_tid;
	}
	private String Exec_repl(byte repl_tid, String text, String regx, int limit) {
		// parse regx
		Regx_adp regx_mgr = Scrib_lib_ustring.RegxAdp_new_(core.Ctx(), regx);
		if (regx_mgr.Pattern_is_invalid()) return text; // NOTE: invalid patterns should return self; EX:[^]; DATE:2014-09-02)

		// exec regx
		Regx_match[] rslts = regx_mgr.Match_all(text, 0);
		if (rslts.length == 0) return text; // PHP: If matches are found, the new subject will be returned, otherwise subject will be returned unchanged.; http://php.net/manual/en/function.preg-replace-callback.php
		rslts = regx_converter.Adjust_balanced(rslts);

		Bry_bfr tmp_bfr = Bry_bfr_.New();
		int rslts_len = rslts.length;
		int text_pos = 0;
		for (int i = 0; i < rslts_len; i++) {
			if (repl_count == limit) break; // stop if repl_count reaches limit; note that limit = -1 by default, unless specified

			// add text up to find.bgn
			Regx_match rslt = rslts[i];
			tmp_bfr.Add_str_u8(String_.Mid(text, text_pos, rslt.Find_bgn()));	// NOTE: regx returns char text_pos (not bry); must add as String, not bry; DATE:2013-07-17
			
			// replace result
			if (!Exec_repl_itm(tmp_bfr, repl_tid, text, rslt)) {
				// will be false when gsub_proc returns nothing; PAGE:en.d:tracer PAGE:en.d:שלום DATE:2017-04-22;
				tmp_bfr.Add_str_u8(String_.Mid(text, rslt.Find_bgn(), rslt.Find_end()));
			}

			// update
			text_pos = rslt.Find_end();
			repl_count++;
		}

		// add rest of String
		int text_len = String_.Len(text);
		if (text_pos < text_len)
			tmp_bfr.Add_str_u8(String_.Mid(text, text_pos, text_len));			// NOTE: regx returns char text_pos (not bry); must add as String, not bry; DATE:2013-07-17
		return tmp_bfr.To_str_and_clear();
	}
	private boolean Exec_repl_itm(Bry_bfr tmp_bfr, byte repl_tid, String text, Regx_match match) {
		switch (repl_tid) {
			case Repl_tid_string:
				int len = repl_bry.length;
				for (int i = 0; i < len; i++) {
					byte b = repl_bry[i];
					switch (b) {
						case Byte_ascii.Percent: {
							++i;
							if (i == len)	// % at end of stream; just add %;
								tmp_bfr.Add_byte(b);							
							else {
								b = repl_bry[i];
								switch (b) {
									case Byte_ascii.Num_0: case Byte_ascii.Num_1: case Byte_ascii.Num_2: case Byte_ascii.Num_3: case Byte_ascii.Num_4:
									case Byte_ascii.Num_5: case Byte_ascii.Num_6: case Byte_ascii.Num_7: case Byte_ascii.Num_8: case Byte_ascii.Num_9:
										int idx = b - Byte_ascii.Num_0;
										if (idx == 0)	// NOTE: 0 means take result; REF.MW:if ($x === '0'); return $m[0]; PAGE:Wikipedia:Wikipedia_Signpost/Templates/Voter/testcases; DATE:2015-08-02
											tmp_bfr.Add_str_u8(String_.Mid(text, match.Find_bgn(), match.Find_end()));											
										else {			// NOTE: > 0 means get from groups if it exists; REF.MW:elseif (isset($m["m$x"])) return $m["m$x"]; PAGE:Wikipedia:Wikipedia_Signpost/Templates/Voter/testcases; DATE:2015-08-02
											idx -= List_adp_.Base1;
											if (idx < match.Groups().length) {	// retrieve numbered capture; TODO_OLD: support more than 9 captures
												Regx_group grp = match.Groups()[idx];
												tmp_bfr.Add_str_u8(String_.Mid(text, grp.Bgn(), grp.End()));	// NOTE: grp.Bgn() / .End() is for String pos (bry pos will fail for utf8 strings)
											}
											else {
												tmp_bfr.Add_byte(Byte_ascii.Percent);
												tmp_bfr.Add_byte(b);
											}
										}
										break;
									case Byte_ascii.Percent:
										tmp_bfr.Add_byte(Byte_ascii.Percent);
										break;
									default:	// not a number; add literal
										tmp_bfr.Add_byte(Byte_ascii.Percent);
										tmp_bfr.Add_byte(b);	
										break;
								}
							}
							break;
						}
						default:
							tmp_bfr.Add_byte(b);
							break;
					}
				}
				break;
			case Repl_tid_table: {
				int match_bgn = -1, match_end = -1;
				Regx_group[] grps = match.Groups();
				if (grps.length == 0) {
					match_bgn = match.Find_bgn();
					match_end = match.Find_end();
				}
				else {	// group exists, take first one (logic matches Scribunto); PAGE:en.w:Bannered_routes_of_U.S._Route_60; DATE:2014-08-15
					Regx_group grp = grps[0];
					match_bgn = grp.Bgn();
					match_end = grp.End();
				}
				String find_str = String_.Mid(text, match_bgn, match_end);	// NOTE: rslt.Bgn() / .End() is for String pos (bry pos will fail for utf8 strings)
				Object actl_repl_obj = repl_hash.Get_by(find_str);
				if (actl_repl_obj == null)			// match found, but no replacement specified; EX:"abc", "[ab]", "a:A"; "b" in regex but not in tbl; EX:d:DVD; DATE:2014-03-31
					tmp_bfr.Add_str_u8(find_str);
				else
					tmp_bfr.Add((byte[])actl_repl_obj);					
				break;
			}
			case Repl_tid_luacbk: {
				Keyval[] luacbk_args = null;
				Regx_group[] grps = match.Groups();
				int grps_len = grps.length;
				// no grps; pass 1 arg based on @match: EX: ("ace", "[b-d]"); args -> ("c")
				if (grps_len == 0) {
					String find_str = String_.Mid(text, match.Find_bgn(), match.Find_end());
					luacbk_args = Scrib_kv_utl_.base1_obj_(find_str);
				}
				// grps exist; pass n args based on grp[n].match; EX: ("acfg", "([b-d])([e-g])"); args -> ("c", "f")
				else {
					// memoize any_pos args for loop
					boolean any_pos = regx_converter.Any_pos();
					Keyval[] capt_ary = regx_converter.Capt_ary();
					int capt_ary_len = capt_ary.length;

					// loop grps; for each grp, create corresponding arg in luacbk
					luacbk_args = new Keyval[grps_len];
					for (int i = 0; i < grps_len; i++) {
						Regx_group grp = grps[i];

						// anypos will create @offset arg; everything else creates a @match arg based on grp
						Object val = any_pos && i < capt_ary_len && Bool_.Cast(capt_ary[i].Val())
								? (Object)grp.Bgn()
								: (Object)String_.Mid(text, grp.Bgn(), grp.End());
						luacbk_args[i] = Keyval_.int_(i + Scrib_core.Base_1, val);
					}
				}

				// do callback
				Keyval[] rslts = core.Interpreter().CallFunction(repl_func.Id(), luacbk_args);

				// eval result
				if (rslts.length == 0) // will be 0 when gsub_proc returns nil; PAGE:en.d:tracer; DATE:2017-04-22
					return false;
				else {									// ArrayIndex check
					Object rslt_obj = rslts[0].Val();	// 0th idx has result
					tmp_bfr.Add_str_u8(Object_.Xto_str_strict_or_empty(rslt_obj));	// NOTE: always convert to String; rslt_obj can be int; PAGE:en.d:seven DATE:2016-04-27
				}
				break;
			}
			default: throw Err_.new_unhandled(repl_tid);
		}
		return true;
	}
	private static final byte Repl_tid_null = 0, Repl_tid_string = 1, Repl_tid_table = 2, Repl_tid_luacbk = 3;
	public static final    Scrib_lib_ustring_gsub_mgr[] Ary_empty = new Scrib_lib_ustring_gsub_mgr[0];
}