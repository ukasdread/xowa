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
package gplx.xowa.mws.parsers.lnkis; import gplx.*; import gplx.xowa.*; import gplx.xowa.mws.*; import gplx.xowa.mws.parsers.*;
import gplx.core.btries.*; import gplx.core.primitives.*;
import gplx.langs.phps.utls.*;
import gplx.xowa.wikis.nss.*; import gplx.xowa.wikis.xwikis.*;
import gplx.xowa.mws.parsers.*; import gplx.xowa.mws.parsers.quotes.*;
import gplx.xowa.mws.htmls.*; import gplx.xowa.mws.linkers.*;
import gplx.xowa.mws.utls.*;
import gplx.xowa.parsers.uniqs.*;
public class Xomw_lnki_wkr {// THREAD.UNSAFE: caching for repeated calls
	private final    Xomw_link_holders holders;
	private final    Xomw_linker linker;
	private final    Xomw_link_renderer link_renderer;
	// private final    Btrie_slim_mgr protocols_trie;
	private final    Xomw_quote_wkr quote_wkr;
	private final    Xomw_strip_state strip_state;
	private Xow_wiki wiki;
	private Xoa_ttl page_title;
	private final    Xomw_linker__normalize_subpage_link normalize_subpage_link = new Xomw_linker__normalize_subpage_link();
	private final    Bry_bfr tmp;
	private final    Xomw_parser parser;
	private final    Xomwh_atr_mgr extra_atrs = new Xomwh_atr_mgr();
	public Xomw_lnki_wkr(Xomw_parser parser, Xomw_link_holders holders, Xomw_link_renderer link_renderer, Btrie_slim_mgr protocols_trie) {
		this.parser = parser;
		this.holders = holders;
		this.link_renderer = link_renderer;
		// this.protocols_trie = protocols_trie;

		this.linker = parser.Linker();
		this.quote_wkr = parser.Quote_wkr();
		this.tmp = parser.Tmp();
		this.strip_state = parser.Strip_state();
	}
	public void Init_by_wiki(Xow_wiki wiki) {
		this.wiki = wiki;
		if (title_chars_for_lnki == null) {
			title_chars_for_lnki = (boolean[])Array_.Clone(Xomw_ttl_utl.Title_chars_valid());
			// the % is needed to support urlencoded titles as well
			title_chars_for_lnki[Byte_ascii.Hash] = true;
			title_chars_for_lnki[Byte_ascii.Percent] = true;
		}
	}
	public void Clear_state() {
		holders.Clear();
	}
	public void Replace_internal_links(Xomw_parser_ctx pctx, Xomw_parser_bfr pbfr) {
		// XO.PBFR
		Bry_bfr src_bfr = pbfr.Src();
		byte[] src = src_bfr.Bfr();
		int src_bgn = 0;
		int src_end = src_bfr.Len();
		Bry_bfr bfr = pbfr.Trg();
		pbfr.Switch();

		this.page_title = pctx.Page_title();

		Replace_internal_links(bfr, src, src_bgn, src_end);
	}
	public void Replace_internal_links(Bry_bfr bfr, byte[] src, int src_bgn, int src_end) {
		// PORTED: regex for tc move to header; e1 and e1_img moved to code
		// split the entire text String on occurrences of [[
		int cur = src_bgn;
		int prv = cur;
		while (true) {
			int lnki_bgn = Bry_find_.Find_fwd(src, Bry__wtxt__lnki__bgn, cur, src_end);	// $a = StringUtils::explode('[[', ' ' . $s);
			if (lnki_bgn == Bry_find_.Not_found) {	// no more "[["; stop loop
				bfr.Add_mid(src, cur, src_end);
				break;
			}
			cur = lnki_bgn + 2;	// 2="[[".length

			// IGNORE: handles strange split logic of adding space to String; "$s = substr($s, 1);"

			// TODO.XO:lnke_bgn; EX: b[[A]]
			// $useLinkPrefixExtension = $this->getTargetLanguage()->linkPrefixExtension();
			// $e2 = null;
			// if ($useLinkPrefixExtension) {
			// 	// Match the end of a line for a word that's not followed by whitespace,
			// 	// e.g. in the case of 'The Arab al[[Razi]]', 'al' will be matched
			// 	global $wgContLang;
			// 	$charset = $wgContLang->linkPrefixCharset();
			// 	$e2 = "/^((?>.*[^$charset]|))(.+)$/sDu";
			// }

			// IGNORE: throw new MWException(__METHOD__ . ": \$this->mTitle is null\n");

			// $nottalk = !$this->mTitle->isTalkPage();

			// TODO.XO:lnke_bgn
			byte[] prefix = Bry_.Empty;
			//if ($useLinkPrefixExtension) {
			//	$m = [];
			//	if (preg_match($e2, $s, $m)) {
			//		$first_prefix = $m[2];
			//	} else {
			//		$first_prefix = false;
			//	}
			//} else {
			//	$prefix = '';
			//}

			// IGNORE: "Check for excessive memory usage"

			// TODO.XO:lnke_bgn; EX: b[[A]]
			//if ($useLinkPrefixExtension) {
			//	if (preg_match($e2, $s, $m)) {
			//		$prefix = $m[2];
			//		$s = $m[1];
			//	} else {
			//		$prefix = '';
			//	}
			//	// first link
			//	if ($first_prefix) {
			//		$prefix = $first_prefix;
			//		$first_prefix = false;
			//	}
			//}

			// PORTED.BGN: if (preg_match($e1, $line, $m)) && else if (preg_match($e1_img, $line, $m))
			// NOTE: both e1 and e1_img are effectively the same; e1_img allows nested "[["; EX: "[[A|b[[c]]d]]" will stop at "[[A|b"
			int ttl_bgn = cur;
			int ttl_end = Xomw_ttl_utl.Find_fwd_while_title(src, cur, src_end, title_chars_for_lnki);
			cur = ttl_end;
			int capt_bgn = -1, capt_end = -1;
			int nxt_lnki = -1;

			boolean might_be_img = false;
			if (ttl_end > ttl_bgn) {	// at least one valid title-char found; check for "|" or "]]" EX: "[[a"
				byte nxt_byte = src[ttl_end];
				if      (nxt_byte == Byte_ascii.Pipe) {	// handles lnki with capt ([[A|a]])and lnki with file ([[File:A.png|b|c|d]])
					cur = ttl_end + 1;

					// find next "[["
					nxt_lnki = Bry_find_.Find_fwd(src, Bry__wtxt__lnki__bgn, cur, src_end);
					if (nxt_lnki == Bry_find_.Not_found)
						nxt_lnki = src_end;

					// find end "]]"
					capt_bgn = cur;
					capt_end = Bry_find_.Find_fwd(src, Bry__wtxt__lnki__end, cur, nxt_lnki);
					if (capt_end == Bry_find_.Not_found) {
						capt_end = nxt_lnki;
						cur = nxt_lnki;
						might_be_img = true;
					}
					else {
						cur = capt_end + Bry__wtxt__lnki__end.length;
					}
				}
				else if (Bry_.Match(src, ttl_end, ttl_end + 2, Bry__wtxt__lnki__end)) {	// handles simple lnki; EX: [[A]]
					cur = ttl_end + 2;
				}
				else {
					ttl_end = -1;
				}
			}
			else
				ttl_end = -1;
			if (ttl_end == -1) { // either (a) no valid title-chars ("[[<") or (b) title char, but has stray "]" ("[[a]b]]")
				// Invalid form; output directly
				bfr.Add_mid(src, cur, src_end);
				continue;
			}
			// PORTED.END: if (preg_match($e1, $line, $m)) && else if (preg_match($e1_img, $line, $m))

			byte[] text = Bry_.Mid(src, capt_bgn, capt_end);
			byte[] trail = Bry_.Empty;
			if (!might_be_img) {
				// If we get a ] at the beginning of $m[3] that means we have a link that's something like:
				// [[Image:Foo.jpg|[http://example.com desc]]] <- having three ] in a row fucks up,
				// the real problem is with the $e1 regex
				// See T1500.
				// Still some problems for cases where the ] is meant to be outside punctuation,
				// and no image is in sight. See T4095.
//					if ($text !== ''
//						&& substr($m[3], 0, 1) === ']'
//						&& strpos($text, '[') !== false
//					) {
//						$text .= ']'; // so that replaceExternalLinks($text) works later
//						$m[3] = substr($m[3], 1);
//					}

				// fix up urlencoded title texts
//					if (strpos($m[1], '%') !== false) {
//						// Should anchors '#' also be rejected?
//						$m[1] = str_replace([ '<', '>' ], [ '&lt;', '&gt;' ], rawurldecode($m[1]));
//					}
//					$trail = $m[3];
			} 
			else {
				// Invalid, but might be an image with a link in its caption
//					$text = $m[2];
//					if (strpos($m[1], '%') !== false) {
//						$m[1] = str_replace([ '<', '>' ], [ '&lt;', '&gt;' ], rawurldecode($m[1]));
//					}
//					$trail = "";
			}

			byte[] orig_link = Bry_.Mid(src, ttl_bgn, ttl_end);

			// TODO.XO: handle "[[http://a.org]]"
			// Don't allow @gplx.Internal protected links to pages containing
			// PROTO: where PROTO is a valid URL protocol; these
			// should be external links.
			// if (preg_match('/^(?i:' . $this->mUrlProtocols . ')/', $origLink)) {
			// 	$s .= $prefix . '[[' . $line;
			//	continue;
			// }

			byte[] link = orig_link;
			boolean no_force = orig_link[0] != Byte_ascii.Colon;
			if (!no_force) {
				// Strip off leading ':'
				link = Bry_.Mid(link, 1);
			}
			Xoa_ttl nt = wiki.Ttl_parse(link);

			// Make subpage if necessary
			boolean subpages_enabled = nt.Ns().Subpages_enabled();
			if (subpages_enabled) {
				Maybe_do_subpage_link(normalize_subpage_link, orig_link, text);
				link = normalize_subpage_link.link;
				text = normalize_subpage_link.text;
				nt = wiki.Ttl_parse(link);
			}
			// IGNORE: handled in rewrite above
			// else {
			//	link = orig_link;
			// }

			byte[] unstrip = strip_state.Unstrip_nowiki(link);
			if (!Bry_.Eq(unstrip, link))
				nt = wiki.Ttl_parse(unstrip);
			if (nt == null) {
				bfr.Add_mid(src, prv, lnki_bgn + 2);	// $s .= $prefix . '[[' . $line;
				cur = lnki_bgn + 2;
				prv = cur;
				continue;
			}

			Xow_ns ns = nt.Ns();
			Xow_xwiki_itm iw = nt.Wik_itm();

			if (might_be_img) { // if this is actually an invalid link
				if (ns.Id_is_file() && no_force) { // but might be an image
					boolean found = false;
//						while (true) {
//							// look at the next 'line' to see if we can close it there
//							a->next();
//							next_line = a->current();
//							if (next_line === false || next_line === null) {
//								break;
//							}
//							m = explode(']]', next_line, 3);
//							if (count(m) == 3) {
//								// the first ]] closes the inner link, the second the image
//								found = true;
//								text .= "[[{m[0]}]]{m[1]}";
//								trail = m[2];
//								break;
//							} else if (count(m) == 2) {
//								// if there's exactly one ]] that's fine, we'll keep looking
//								text .= "[[{m[0]}]]{m[1]}";
//							} else {
//								// if next_line is invalid too, we need look no further
//								text .= '[[' . next_line;
//								break;
//							}
//						}
					if (!found) {
						// we couldn't find the end of this imageLink, so output it raw
						// but don't ignore what might be perfectly normal links in the text we've examined
						Bry_bfr nested = wiki.Utl__bfr_mkr().Get_b128();
						this.Replace_internal_links(nested, text, 0, text.length);
						nested.Mkr_rls();
						bfr.Add(prefix).Add(Bry__wtxt__lnki__bgn).Add(link).Add_byte_pipe().Add(text); // s .= "{prefix}[[link|text";
						// note: no trail, because without an end, there *is* no trail
						continue;
					}
				}
				else { // it's not an image, so output it raw
					bfr.Add(prefix).Add(Bry__wtxt__lnki__bgn).Add(link).Add_byte_pipe().Add(text); // s .= "{prefix}[[link|text";
					// note: no trail, because without an end, there *is* no trail
					continue;
				}
			}

			boolean was_blank = text.length == 0;
			if (was_blank) {
				text = link;
			} 
			else {
				// T6598 madness. Handle the quotes only if they come from the alternate part
				// [[Lista d''e paise d''o munno]] -> <a href="...">Lista d''e paise d''o munno</a>
				// [[Criticism of Harry Potter|Criticism of ''Harry Potter'']]
				//    -> <a href="Criticism of Harry Potter">Criticism of <i>Harry Potter</i></a>
				text = quote_wkr.Do_quotes(tmp, text);
			}

			// Link not escaped by : , create the various objects
//				if (no_force && !nt->wasLocalInterwiki()) {
				// Interwikis
//					if (
//						iw && this->mOptions->getInterwikiMagic() && nottalk && (
//							Language::fetchLanguageName(iw, null, 'mw') ||
//							in_array(iw, wgExtraInterlanguageLinkPrefixes)
//						)
//					) {
					// T26502: filter duplicates
//						if (!isset(this->mLangLinkLanguages[iw])) {
//							this->mLangLinkLanguages[iw] = true;
//							this->mOutput->addLanguageLink(nt->getFullText());
//						}
//
//						s = rtrim(s . prefix);
//						s .= trim(trail, "\n") == '' ? '': prefix . trail;
//						continue;
//					}
//
				if (ns.Id_is_file()) {
//						if (!wfIsBadImage(nt->getDBkey(), this->mTitle)) {
//							if (wasblank) {
//								// if no parameters were passed, text
//								// becomes something like "File:Foo.png",
//								// which we don't want to pass on to the
//								// image generator
//								text = '';
//							} else {
//								// recursively parse links inside the image caption
//								// actually, this will parse them in any other parameters, too,
//								// but it might be hard to fix that, and it doesn't matter ATM
//								text = this->replaceExternalLinks(text);
//								holders->merge(this->replaceInternalLinks2(text));
//							}
//							// cloak any absolute URLs inside the image markup, so replaceExternalLinks() won't touch them
//							s .= prefix . this->armorLinks(
//								this->makeImage(nt, text, holders)) . trail;
//							continue;
//						}
				} 
				else if (ns.Id_is_ctg()) {
					bfr.Trim_end_ws(); // s = rtrim(s . "\n"); // T2087

					if (was_blank) {
//							sortkey = this->getDefaultSort();
					}
					else {
//							sortkey = text;
					}
//						sortkey = Sanitizer::decodeCharReferences(sortkey);
//						sortkey = str_replace("\n", '', sortkey);
//						sortkey = this->getConverterLanguage()->convertCategoryKey(sortkey);
//						this->mOutput->addCategory(nt->getDBkey(), sortkey);
//
					// Strip the whitespace Category links produce, see T2087
//						s .= trim(prefix . trail, "\n") == '' ? '' : prefix . trail;

					continue;
				}
//				}

			// Self-link checking. For some languages, variants of the title are checked in
			// LinkHolderArray::doVariants() to allow batching the existence checks necessary
			// for linking to a different variant.
			if (!ns.Id_is_special() && nt.Eq_full_db(page_title) && !nt.Has_fragment()) {
				bfr.Add(prefix);
				linker.Make_self_link_obj(bfr, nt, text, Bry_.Empty, trail, Bry_.Empty);
				continue;
			}

			// NS_MEDIA is a pseudo-namespace for linking directly to a file
			// @todo FIXME: Should do batch file existence checks, see comment below
			if (ns.Id_is_media()) {
				// Give extensions a chance to select the file revision for us
//					options = [];
//					descQuery = false;
				// MW.HOOK:BeforeParserFetchFileAndTitle
				// Fetch and register the file (file title may be different via hooks)
//					list(file, nt) = this->fetchFileAndTitle(nt, options);
				// Cloak with NOPARSE to avoid replacement in replaceExternalLinks
//					s .= prefix . this->armorLinks(
//						Linker::makeMediaLinkFile(nt, file, text)) . trail;
//					continue;
			}

			// Some titles, such as valid special pages or files in foreign repos, should
			// be shown as bluelinks even though they're not included in the page table
			// @todo FIXME: isAlwaysKnown() can be expensive for file links; we should really do
			// batch file existence checks for NS_FILE and NS_MEDIA
			bfr.Add_mid(src, prv, lnki_bgn);
			prv = cur;
			if (iw == null && nt.Is_always_known()) {
				// this->mOutput->addLink(nt);
				Make_known_link_holder(bfr, nt, text, trail, prefix);
			}
			else {
				// Links will be added to the output link list after checking
				holders.Make_holder(bfr, nt, text, Bry_.Ary_empty, trail, prefix);
			}
		}
	}
	public void Maybe_do_subpage_link(Xomw_linker__normalize_subpage_link rv, byte[] target, byte[] text) {
		linker.Normalize_subpage_link(rv, page_title, target, text);
	}
	public void Replace_link_holders(Xomw_parser_ctx pctx, Xomw_parser_bfr pbfr) {
		holders.Replace(pctx, pbfr);
	}
	public void Make_known_link_holder(Bry_bfr bfr, Xoa_ttl nt, byte[] text, byte[] trail, byte[] prefix) {
		byte[][] split_trail = linker.Split_trail(trail);
		byte[] inside = split_trail[0];
		trail = split_trail[1];

		if (text == Bry_.Empty) {
			text = Bry_.Escape_html(nt.Get_prefixed_text()); 
		}

		// PORTED:new HtmlArmor( "$prefix$text$inside" )
		tmp.Add_bry_escape_html(prefix);
		tmp.Add_bry_escape_html(text);
		tmp.Add_bry_escape_html(inside);
		text = tmp.To_bry_and_clear();
		
		link_renderer.Make_known_link(bfr, nt, text, extra_atrs, Bry_.Empty);
		byte[] link = bfr.To_bry_and_clear();
		parser.Armor_links(bfr, link, 0, link.length);
		bfr.Add(trail);
	}

	private static boolean[] title_chars_for_lnki;
	private static final    byte[] Bry__wtxt__lnki__bgn = Bry_.new_a7("[["), Bry__wtxt__lnki__end = Bry_.new_a7("]]");

	// $e1 = "/^([{$tc}]+)(?:\\|(.+?))?]](.*)\$/sD";
	// 
	// REGEX: "title-char"(1+) + "pipe"(0-1) + "]]"(0-1) + "other chars up to next [["
	//   title-char             -> ([{$tc}]+)
	//   pipe                   -> (?:\\|(.+?))?
	//   ]]                     -> ?]]
	//   other chars...         -> (.*)

	// $e1_img = "/^([{$tc}]+)\\|(.*)\$/sD";
	// 
	// REGEX: "title-char"(1+) + "pipe"(0-1) + "other chars up to next [["
	//   title-char             -> ([{$tc}]+)
	//   pipe                   -> \\|
	//   other chars...         -> (.*)
}
