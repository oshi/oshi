/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.unix.LibCAPI;
import com.sun.jna.ptr.PointerByReference;

/**
 * C library with code common to all *nix-based operating systems. This class
 * should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 */
public interface CLibrary extends LibCAPI, Library {

    int AI_CANONNAME = 2;

    int UT_LINESIZE = 32;
    int UT_NAMESIZE = 32;
    int UT_HOSTSIZE = 256;
    int LOGIN_PROCESS = 6; // Session leader of a logged in user.
    int USER_PROCESS = 7; // Normal process.

    @FieldOrder({ "sa_family", "sa_data" })
    class Sockaddr extends Structure {
        public short sa_family;
        public byte[] sa_data = new byte[14];

        public static class ByReference extends Sockaddr implements Structure.ByReference {
        }
    }

    @FieldOrder({ "ai_flags", "ai_family", "ai_socktype", "ai_protocol", "ai_addrlen", "ai_addr", "ai_canonname",
            "ai_next" })
    class Addrinfo extends Structure {
        public int ai_flags;
        public int ai_family;
        public int ai_socktype;
        public int ai_protocol;
        public int ai_addrlen;
        public Sockaddr.ByReference ai_addr;
        public String ai_canonname;
        public ByReference ai_next;

        public static class ByReference extends Addrinfo implements Structure.ByReference {
        }

        public Addrinfo() {
        }

        public Addrinfo(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * Return type for sysctl net.inet.ip.stats
     */
    @FieldOrder({ "tcps_connattempt", "tcps_accepts", "tcps_connects", "tcps_drops", "tcps_conndrops", "tcps_closed",
            "tcps_segstimed", "tcps_rttupdated", "tcps_delack", "tcps_timeoutdrop", "tcps_rexmttimeo",
            "tcps_persisttimeo", "tcps_keeptimeo", "tcps_keepprobe", "tcps_keepdrops", "tcps_sndtotal", "tcps_sndpack",
            "tcps_sndbyte", "tcps_sndrexmitpack", "tcps_sndrexmitbyte", "tcps_sndacks", "tcps_sndprobe", "tcps_sndurg",
            "tcps_sndwinup", "tcps_sndctrl", "tcps_rcvtotal", "tcps_rcvpack", "tcps_rcvbyte", "tcps_rcvbadsum",
            "tcps_rcvbadoff", "tcps_rcvmemdrop", "tcps_rcvshort", "tcps_rcvduppack", "tcps_rcvdupbyte",
            "tcps_rcvpartduppack", "tcps_rcvpartdupbyte", "tcps_rcvoopack", "tcps_rcvoobyte", "tcps_rcvpackafterwin",
            "tcps_rcvbyteafterwin", "tcps_rcvafterclose", "tcps_rcvwinprobe", "tcps_rcvdupack", "tcps_rcvacktoomuch",
            "tcps_rcvackpack", "tcps_rcvackbyte", "tcps_rcvwinupd", "tcps_pawsdrop", "tcps_predack", "tcps_preddat",
            "tcps_pcbcachemiss", "tcps_cachedrtt", "tcps_cachedrttvar", "tcps_cachedssthresh", "tcps_usedrtt",
            "tcps_usedrttvar", "tcps_usedssthresh", "tcps_persistdrop", "tcps_badsyn", "tcps_mturesent",
            "tcps_listendrop", "tcps_synchallenge", "tcps_rstchallenge", "tcps_minmssdrops", "tcps_sndrexmitbad",
            "tcps_badrst", "tcps_sc_added", "tcps_sc_retransmitted", "tcps_sc_dupsyn", "tcps_sc_dropped",
            "tcps_sc_completed", "tcps_sc_bucketoverflow", "tcps_sc_cacheoverflow", "tcps_sc_reset", "tcps_sc_stale",
            "tcps_sc_aborted", "tcps_sc_badack", "tcps_sc_unreach", "tcps_sc_zonefail", "tcps_sc_sendcookie",
            "tcps_sc_recvcookie", "tcps_hc_added", "tcps_hc_bucketoverflow", "tcps_sack_recovery_episode",
            "tcps_sack_rexmits", "tcps_sack_rexmit_bytes", "tcps_sack_rcv_blocks", "tcps_sack_send_blocks",
            "tcps_sack_sboverflow", "tcps_bg_rcvtotal", "tcps_rxtfindrop", "tcps_fcholdpacket", "tcps_coalesced_pack",
            "tcps_flowtbl_full", "tcps_flowtbl_collision", "tcps_lro_twopack", "tcps_lro_multpack",
            "tcps_lro_largepack", "tcps_limited_txt", "tcps_early_rexmt", "tcps_sack_ackadv", "tcps_rcv_swcsum",
            "tcps_rcv_swcsum_bytes", "tcps_rcv6_swcsum", "tcps_rcv6_swcsum_bytes", "tcps_snd_swcsum",
            "tcps_snd_swcsum_bytes", "tcps_snd6_swcsum", "tcps_snd6_swcsum_bytes", "tcps_msg_unopkts",
            "tcps_msg_unoappendfail", "tcps_msg_sndwaithipri", "tcps_invalid_mpcap", "tcps_invalid_joins",
            "tcps_mpcap_fallback", "tcps_join_fallback", "tcps_estab_fallback", "tcps_invalid_opt", "tcps_mp_outofwin",
            "tcps_mp_reducedwin", "tcps_mp_badcsum", "tcps_mp_oodata", "tcps_mp_switches", "tcps_mp_rcvtotal",
            "tcps_mp_rcvbytes", "tcps_mp_sndpacks", "tcps_mp_sndbytes", "tcps_join_rxmts", "tcps_tailloss_rto",
            "tcps_reordered_pkts", "tcps_recovered_pkts", "tcps_pto", "tcps_rto_after_pto", "tcps_tlp_recovery",
            "tcps_tlp_recoverlastpkt", "tcps_ecn_client_success", "tcps_ecn_recv_ece", "tcps_ecn_sent_ece",
            "tcps_detect_reordering", "tcps_delay_recovery", "tcps_avoid_rxmt", "tcps_unnecessary_rxmt",
            "tcps_nostretchack", "tcps_rescue_rxmt", "tcps_pto_in_recovery", "tcps_pmtudbh_reverted",
            "tcps_dsack_disable", "tcps_dsack_ackloss", "tcps_dsack_badrexmt", "tcps_dsack_sent", "tcps_dsack_recvd",
            "tcps_dsack_recvd_old", "tcps_mp_sel_symtomsd", "tcps_mp_sel_rtt", "tcps_mp_sel_rto", "tcps_mp_sel_peer",
            "tcps_mp_num_probes", "tcps_mp_verdowngrade", "tcps_drop_after_sleep", "tcps_probe_if",
            "tcps_probe_if_conflict", "tcps_ecn_client_setup", "tcps_ecn_server_setup", "tcps_ecn_server_success",
            "tcps_ecn_lost_synack", "tcps_ecn_lost_syn", "tcps_ecn_not_supported", "tcps_ecn_recv_ce",
            "tcps_ecn_conn_recv_ce", "tcps_ecn_conn_recv_ece", "tcps_ecn_conn_plnoce", "tcps_ecn_conn_pl_ce",
            "tcps_ecn_conn_nopl_ce", "tcps_ecn_fallback_synloss", "tcps_ecn_fallback_reorder", "tcps_ecn_fallback_ce",
            "tcps_tfo_syn_data_rcv", "tcps_tfo_cookie_req_rcv", "tcps_tfo_cookie_sent", "tcps_tfo_cookie_invalid",
            "tcps_tfo_cookie_req", "tcps_tfo_cookie_rcv", "tcps_tfo_syn_data_sent", "tcps_tfo_syn_data_acked",
            "tcps_tfo_syn_loss", "tcps_tfo_blackhole", "tcps_tfo_cookie_wrong", "tcps_tfo_no_cookie_rcv",
            "tcps_tfo_heuristics_disable", "tcps_tfo_sndblackhole", "tcps_mss_to_default", "tcps_mss_to_medium",
            "tcps_mss_to_low", "tcps_ecn_fallback_droprst", "tcps_ecn_fallback_droprxmt", "tcps_ecn_fallback_synrst",
            "tcps_mptcp_rcvmemdrop", "tcps_mptcp_rcvduppack", "tcps_mptcp_rcvpackafterwin", "tcps_timer_drift_le_1_ms",
            "tcps_timer_drift_le_10_ms", "tcps_timer_drift_le_20_ms", "tcps_timer_drift_le_50_ms",
            "tcps_timer_drift_le_100_ms", "tcps_timer_drift_le_200_ms", "tcps_timer_drift_le_500_ms",
            "tcps_timer_drift_le_1000_ms", "tcps_timer_drift_gt_1000_ms", "tcps_mptcp_handover_attempt",
            "tcps_mptcp_interactive_attempt", "tcps_mptcp_aggregate_attempt", "tcps_mptcp_fp_handover_attempt",
            "tcps_mptcp_fp_interactive_attempt", "tcps_mptcp_fp_aggregate_attempt", "tcps_mptcp_heuristic_fallback",
            "tcps_mptcp_fp_heuristic_fallback", "tcps_mptcp_handover_success_wifi", "tcps_mptcp_handover_success_cell",
            "tcps_mptcp_interactive_success", "tcps_mptcp_aggregate_success", "tcps_mptcp_fp_handover_success_wifi",
            "tcps_mptcp_fp_handover_success_cell", "tcps_mptcp_fp_interactive_success",
            "tcps_mptcp_fp_aggregate_success", "tcps_mptcp_handover_cell_from_wifi",
            "tcps_mptcp_handover_wifi_from_cell", "tcps_mptcp_interactive_cell_from_wifi",
            "tcps_mptcp_handover_cell_bytes", "tcps_mptcp_interactive_cell_bytes", "tcps_mptcp_aggregate_cell_bytes",
            "tcps_mptcp_handover_all_bytes", "tcps_mptcp_interactive_all_bytes", "tcps_mptcp_aggregate_all_bytes",
            "tcps_mptcp_back_to_wifi", "tcps_mptcp_wifi_proxy", "tcps_mptcp_cell_proxy", "tcps_ka_offload_drops",
            "tcps_mptcp_triggered_cell" })
    class Tcpstat extends Structure {
        public int tcps_connattempt;
        public int tcps_accepts;
        public int tcps_connects;
        public int tcps_drops;
        public int tcps_conndrops;
        public int tcps_closed;
        public int tcps_segstimed;
        public int tcps_rttupdated;
        public int tcps_delack;
        public int tcps_timeoutdrop;
        public int tcps_rexmttimeo;
        public int tcps_persisttimeo;
        public int tcps_keeptimeo;
        public int tcps_keepprobe;
        public int tcps_keepdrops;
        public int tcps_sndtotal;
        public int tcps_sndpack;
        public int tcps_sndbyte;
        public int tcps_sndrexmitpack;
        public int tcps_sndrexmitbyte;
        public int tcps_sndacks;
        public int tcps_sndprobe;
        public int tcps_sndurg;
        public int tcps_sndwinup;
        public int tcps_sndctrl;
        public int tcps_rcvtotal;
        public int tcps_rcvpack;
        public int tcps_rcvbyte;
        public int tcps_rcvbadsum;
        public int tcps_rcvbadoff;
        public int tcps_rcvmemdrop;
        public int tcps_rcvshort;
        public int tcps_rcvduppack;
        public int tcps_rcvdupbyte;
        public int tcps_rcvpartduppack;
        public int tcps_rcvpartdupbyte;
        public int tcps_rcvoopack;
        public int tcps_rcvoobyte;
        public int tcps_rcvpackafterwin;
        public int tcps_rcvbyteafterwin;
        public int tcps_rcvafterclose;
        public int tcps_rcvwinprobe;
        public int tcps_rcvdupack;
        public int tcps_rcvacktoomuch;
        public int tcps_rcvackpack;
        public int tcps_rcvackbyte;
        public int tcps_rcvwinupd;
        public int tcps_pawsdrop;
        public int tcps_predack;
        public int tcps_preddat;
        public int tcps_pcbcachemiss;
        public int tcps_cachedrtt;
        public int tcps_cachedrttvar;
        public int tcps_cachedssthresh;
        public int tcps_usedrtt;
        public int tcps_usedrttvar;
        public int tcps_usedssthresh;
        public int tcps_persistdrop;
        public int tcps_badsyn;
        public int tcps_mturesent;
        public int tcps_listendrop;
        public int tcps_synchallenge;
        public int tcps_rstchallenge;
        public int tcps_minmssdrops;
        public int tcps_sndrexmitbad;
        public int tcps_badrst;
        public int tcps_sc_added;
        public int tcps_sc_retransmitted;
        public int tcps_sc_dupsyn;
        public int tcps_sc_dropped;
        public int tcps_sc_completed;
        public int tcps_sc_bucketoverflow;
        public int tcps_sc_cacheoverflow;
        public int tcps_sc_reset;
        public int tcps_sc_stale;
        public int tcps_sc_aborted;
        public int tcps_sc_badack;
        public int tcps_sc_unreach;
        public int tcps_sc_zonefail;
        public int tcps_sc_sendcookie;
        public int tcps_sc_recvcookie;
        public int tcps_hc_added;
        public int tcps_hc_bucketoverflow;
        public int tcps_sack_recovery_episode;
        public int tcps_sack_rexmits;
        public int tcps_sack_rexmit_bytes;
        public int tcps_sack_rcv_blocks;
        public int tcps_sack_send_blocks;
        public int tcps_sack_sboverflow;
        public int tcps_bg_rcvtotal;
        public int tcps_rxtfindrop;
        public int tcps_fcholdpacket;
        public int tcps_coalesced_pack;
        public int tcps_flowtbl_full;
        public int tcps_flowtbl_collision;
        public int tcps_lro_twopack;
        public int tcps_lro_multpack;
        public int tcps_lro_largepack;
        public int tcps_limited_txt;
        public int tcps_early_rexmt;
        public int tcps_sack_ackadv;
        public int tcps_rcv_swcsum;
        public int tcps_rcv_swcsum_bytes;
        public int tcps_rcv6_swcsum;
        public int tcps_rcv6_swcsum_bytes;
        public int tcps_snd_swcsum;
        public int tcps_snd_swcsum_bytes;
        public int tcps_snd6_swcsum;
        public int tcps_snd6_swcsum_bytes;
        public int tcps_msg_unopkts;
        public int tcps_msg_unoappendfail;
        public int tcps_msg_sndwaithipri;
        public int tcps_invalid_mpcap;
        public int tcps_invalid_joins;
        public int tcps_mpcap_fallback;
        public int tcps_join_fallback;
        public int tcps_estab_fallback;
        public int tcps_invalid_opt;
        public int tcps_mp_outofwin;
        public int tcps_mp_reducedwin;
        public int tcps_mp_badcsum;
        public int tcps_mp_oodata;
        public int tcps_mp_switches;
        public int tcps_mp_rcvtotal;
        public int tcps_mp_rcvbytes;
        public int tcps_mp_sndpacks;
        public int tcps_mp_sndbytes;
        public int tcps_join_rxmts;
        public int tcps_tailloss_rto;
        public int tcps_reordered_pkts;
        public int tcps_recovered_pkts;
        public int tcps_pto;
        public int tcps_rto_after_pto;
        public int tcps_tlp_recovery;
        public int tcps_tlp_recoverlastpkt;
        public int tcps_ecn_client_success;
        public int tcps_ecn_recv_ece;
        public int tcps_ecn_sent_ece;
        public int tcps_detect_reordering;
        public int tcps_delay_recovery;
        public int tcps_avoid_rxmt;
        public int tcps_unnecessary_rxmt;
        public int tcps_nostretchack;
        public int tcps_rescue_rxmt;
        public int tcps_pto_in_recovery;
        public int tcps_pmtudbh_reverted;
        public int tcps_dsack_disable;
        public int tcps_dsack_ackloss;
        public int tcps_dsack_badrexmt;
        public int tcps_dsack_sent;
        public int tcps_dsack_recvd;
        public int tcps_dsack_recvd_old;
        public int tcps_mp_sel_symtomsd;
        public int tcps_mp_sel_rtt;
        public int tcps_mp_sel_rto;
        public int tcps_mp_sel_peer;
        public int tcps_mp_num_probes;
        public int tcps_mp_verdowngrade;
        public int tcps_drop_after_sleep;
        public int tcps_probe_if;
        public int tcps_probe_if_conflict;
        public int tcps_ecn_client_setup;
        public int tcps_ecn_server_setup;
        public int tcps_ecn_server_success;
        public int tcps_ecn_lost_synack;
        public int tcps_ecn_lost_syn;
        public int tcps_ecn_not_supported;
        public int tcps_ecn_recv_ce;
        public int tcps_ecn_conn_recv_ce;
        public int tcps_ecn_conn_recv_ece;
        public int tcps_ecn_conn_plnoce;
        public int tcps_ecn_conn_pl_ce;
        public int tcps_ecn_conn_nopl_ce;
        public int tcps_ecn_fallback_synloss;
        public int tcps_ecn_fallback_reorder;
        public int tcps_ecn_fallback_ce;
        public int tcps_tfo_syn_data_rcv;
        public int tcps_tfo_cookie_req_rcv;
        public int tcps_tfo_cookie_sent;
        public int tcps_tfo_cookie_invalid;
        public int tcps_tfo_cookie_req;
        public int tcps_tfo_cookie_rcv;
        public int tcps_tfo_syn_data_sent;
        public int tcps_tfo_syn_data_acked;
        public int tcps_tfo_syn_loss;
        public int tcps_tfo_blackhole;
        public int tcps_tfo_cookie_wrong;
        public int tcps_tfo_no_cookie_rcv;
        public int tcps_tfo_heuristics_disable;
        public int tcps_tfo_sndblackhole;
        public int tcps_mss_to_default;
        public int tcps_mss_to_medium;
        public int tcps_mss_to_low;
        public int tcps_ecn_fallback_droprst;
        public int tcps_ecn_fallback_droprxmt;
        public int tcps_ecn_fallback_synrst;
        public int tcps_mptcp_rcvmemdrop;
        public int tcps_mptcp_rcvduppack;
        public int tcps_mptcp_rcvpackafterwin;
        public int tcps_timer_drift_le_1_ms;
        public int tcps_timer_drift_le_10_ms;
        public int tcps_timer_drift_le_20_ms;
        public int tcps_timer_drift_le_50_ms;
        public int tcps_timer_drift_le_100_ms;
        public int tcps_timer_drift_le_200_ms;
        public int tcps_timer_drift_le_500_ms;
        public int tcps_timer_drift_le_1000_ms;
        public int tcps_timer_drift_gt_1000_ms;
        public int tcps_mptcp_handover_attempt;
        public int tcps_mptcp_interactive_attempt;
        public int tcps_mptcp_aggregate_attempt;
        public int tcps_mptcp_fp_handover_attempt;
        public int tcps_mptcp_fp_interactive_attempt;
        public int tcps_mptcp_fp_aggregate_attempt;
        public int tcps_mptcp_heuristic_fallback;
        public int tcps_mptcp_fp_heuristic_fallback;
        public int tcps_mptcp_handover_success_wifi;
        public int tcps_mptcp_handover_success_cell;
        public int tcps_mptcp_interactive_success;
        public int tcps_mptcp_aggregate_success;
        public int tcps_mptcp_fp_handover_success_wifi;
        public int tcps_mptcp_fp_handover_success_cell;
        public int tcps_mptcp_fp_interactive_success;
        public int tcps_mptcp_fp_aggregate_success;
        public int tcps_mptcp_handover_cell_from_wifi;
        public int tcps_mptcp_handover_wifi_from_cell;
        public int tcps_mptcp_interactive_cell_from_wifi;
        public long tcps_mptcp_handover_cell_bytes;
        public long tcps_mptcp_interactive_cell_bytes;
        public long tcps_mptcp_aggregate_cell_bytes;
        public long tcps_mptcp_handover_all_bytes;
        public long tcps_mptcp_interactive_all_bytes;
        public long tcps_mptcp_aggregate_all_bytes;
        public int tcps_mptcp_back_to_wifi;
        public int tcps_mptcp_wifi_proxy;
        public int tcps_mptcp_cell_proxy;
        public int tcps_ka_offload_drops;
        public int tcps_mptcp_triggered_cell;
    }

    /**
     * Return type for sysctl net.inet.ip.stats
     */
    @FieldOrder({ "ips_total", "ips_badsum", "ips_tooshort", "ips_toosmall", "ips_badhlen", "ips_badlen",
            "ips_fragments", "ips_fragdropped", "ips_fragtimeout", "ips_forward", "ips_fastforward", "ips_cantforward",
            "ips_redirectsent", "ips_noproto", "ips_delivered", "ips_localout", "ips_odropped", "ips_reassembled",
            "ips_fragmented", "ips_ofragments", "ips_cantfrag", "ips_badoptions", "ips_noroute", "ips_badvers",
            "ips_rawout", "ips_toolong", "ips_notmember", "ips_nogif", "ips_badaddr", "ips_pktdropcntrl",
            "ips_rcv_swcsum", "ips_rcv_swcsum_bytes", "ips_snd_swcsum", "ips_snd_swcsum_bytes", "ips_adj",
            "ips_adj_hwcsum_clr", "ips_rxc_collisions", "ips_rxc_chained", "ips_rxc_notchain", "ips_rxc_chainsz_gt2",
            "ips_rxc_chainsz_gt4", "ips_rxc_notlist", "ips_raw_sappend_fail", "ips_necp_policy_drop" })
    class Ipstat extends Structure {
        public int ips_total;
        public int ips_badsum;
        public int ips_tooshort;
        public int ips_toosmall;
        public int ips_badhlen;
        public int ips_badlen;
        public int ips_fragments;
        public int ips_fragdropped;
        public int ips_fragtimeout;
        public int ips_forward;
        public int ips_fastforward;
        public int ips_cantforward;
        public int ips_redirectsent;
        public int ips_noproto;
        public int ips_delivered;
        public int ips_localout;
        public int ips_odropped;
        public int ips_reassembled;
        public int ips_fragmented;
        public int ips_ofragments;
        public int ips_cantfrag;
        public int ips_badoptions;
        public int ips_noroute;
        public int ips_badvers;
        public int ips_rawout;
        public int ips_toolong;
        public int ips_notmember;
        public int ips_nogif;
        public int ips_badaddr;
        public int ips_pktdropcntrl;
        public int ips_rcv_swcsum;
        public int ips_rcv_swcsum_bytes;
        public int ips_snd_swcsum;
        public int ips_snd_swcsum_bytes;
        public int ips_adj;
        public int ips_adj_hwcsum_clr;
        public int ips_rxc_collisions;
        public int ips_rxc_chained;
        public int ips_rxc_notchain;
        public int ips_rxc_chainsz_gt2;
        public int ips_rxc_chainsz_gt4;
        public int ips_rxc_notlist;
        public int ips_raw_sappend_fail;
        public int ips_necp_policy_drop;
    }

    /**
     * Return type for sysctl net.inet6.ip6.stats
     */
    @FieldOrder({ "ip6s_total", "ip6s_tooshort", "ip6s_toosmall", "ip6s_fragments", "ip6s_fragdropped",
            "ip6s_fragtimeout", "ip6s_fragoverflow", "ip6s_forward", "ip6s_cantforward", "ip6s_redirectsent",
            "ip6s_delivered", "ip6s_localout", "ip6s_odropped", "ip6s_reassembled", "ip6s_fragmented",
            "ip6s_ofragments", "ip6s_cantfrag", "ip6s_badoptions", "ip6s_noroute", "ip6s_badvers", "ip6s_rawout",
            "ip6s_badscope", "ip6s_notmember", "ip6s_nxthist", "ip6s_m1", "ip6s_m2m", "ip6s_mext1", "ip6s_mext2m",
            "ip6s_exthdrtoolong", "ip6s_nogif", "ip6s_toomanyhdr", "ip6s_exthdrget", "ip6s_exthdrget0", "ip6s_pulldown",
            "ip6s_pulldown_copy", "ip6s_pulldown_alloc", "ip6s_pullup", "ip6s_pullup_copy", "ip6s_pullup_alloc",
            "ip6s_pullup_fail", "ip6s_pullup2", "ip6s_pullup2_copy", "ip6s_pullup2_alloc", "ip6s_pullup2_fail",
            "ip6s_sources_none", "ip6s_sources_sameif", "ip6s_sources_otherif", "ip6s_sources_samescope",
            "ip6s_sources_otherscope", "ip6s_sources_deprecated" })
    class Ip6stat extends Structure {
        public long ip6s_total;
        public long ip6s_tooshort;
        public long ip6s_toosmall;
        public long ip6s_fragments;
        public long ip6s_fragdropped;
        public long ip6s_fragtimeout;
        public long ip6s_fragoverflow;
        public long ip6s_forward;
        public long ip6s_cantforward;
        public long ip6s_redirectsent;
        public long ip6s_delivered;
        public long ip6s_localout;
        public long ip6s_odropped;
        public long ip6s_reassembled;
        public long ip6s_fragmented;
        public long ip6s_ofragments;
        public long ip6s_cantfrag;
        public long ip6s_badoptions;
        public long ip6s_noroute;
        public long ip6s_badvers;
        public long ip6s_rawout;
        public long ip6s_badscope;
        public long ip6s_notmember;
        public long[] ip6s_nxthist = new long[256];
        public long ip6s_m1;
        public long[] ip6s_m2m = new long[32];
        public long ip6s_mext1;
        public long ip6s_mext2m;
        public long ip6s_exthdrtoolong;
        public long ip6s_nogif;
        public long ip6s_toomanyhdr;
        public long ip6s_exthdrget;
        public long ip6s_exthdrget0;
        public long ip6s_pulldown;
        public long ip6s_pulldown_copy;
        public long ip6s_pulldown_alloc;
        public long ip6s_pullup;
        public long ip6s_pullup_copy;
        public long ip6s_pullup_alloc;
        public long ip6s_pullup_fail;
        public long ip6s_pullup2;
        public long ip6s_pullup2_copy;
        public long ip6s_pullup2_alloc;
        public long ip6s_pullup2_fail;
        public long ip6s_sources_none;
        public long[] ip6s_sources_sameif = new long[16];
        public long[] ip6s_sources_otherif = new long[16];
        public long[] ip6s_sources_samescope = new long[16];
        public long[] ip6s_sources_otherscope = new long[16];
        public long[] ip6s_sources_deprecated = new long[16];
    }

    /**
     * Return type for sysctl net.inet.udp.stats
     */
    @FieldOrder({ "udps_ipackets", "udps_hdrops", "udps_badsum", "udps_badlen", "udps_noport", "udps_noportbcast",
            "udps_fullsock", "udpps_pcbcachemiss", "udpps_pcbhashmiss", "udps_opackets", "udps_fastout", "udps_nosum",
            "udps_noportmcast", "udps_filtermcast", "udps_rcv_swcsum", "udps_rcv_swcsum_bytes", "udps_rcv6_swcsum",
            "udps_rcv6_swcsum_bytes", "udps_snd_swcsum", "udps_snd_swcsum_bytes", "udps_snd6_swcsum",
            "udps_snd6_swcsum_bytes" })
    class Udpstat extends Structure {
        public int udps_ipackets;
        public int udps_hdrops;
        public int udps_badsum;
        public int udps_badlen;
        public int udps_noport;
        public int udps_noportbcast;
        public int udps_fullsock;
        public int udpps_pcbcachemiss;
        public int udpps_pcbhashmiss;
        public int udps_opackets;
        public int udps_fastout;
        public int udps_nosum;
        public int udps_noportmcast;
        public int udps_filtermcast;
        public int udps_rcv_swcsum;
        public int udps_rcv_swcsum_bytes;
        public int udps_rcv6_swcsum;
        public int udps_rcv6_swcsum_bytes;
        public int udps_snd_swcsum;
        public int udps_snd_swcsum_bytes;
        public int udps_snd6_swcsum;
        public int udps_snd6_swcsum_bytes;
    }

    /**
     * Returns the process ID of the calling process. The ID is guaranteed to be
     * unique and is useful for constructing temporary file names.
     *
     * @return the process ID of the calling process.
     */
    int getpid();

    /**
     * Given node and service, which identify an Internet host and a service,
     * getaddrinfo() returns one or more addrinfo structures, each of which contains
     * an Internet address that can be specified in a call to bind(2) or connect(2).
     *
     * @param node
     *            a numerical network address or a network hostname, whose network
     *            addresses are looked up and resolved.
     * @param service
     *            sets the port in each returned address structure.
     * @param hints
     *            specifies criteria for selecting the socket address structures
     *            returned in the list pointed to by res.
     * @param res
     *            returned address structure
     * @return 0 on success; sets errno on failure
     */
    int getaddrinfo(String node, String service, Addrinfo hints, PointerByReference res);

    /**
     * Frees the memory that was allocated for the dynamically allocated linked list
     * res.
     *
     * @param res
     *            Pointer to linked list returned by getaddrinfo
     */
    void freeaddrinfo(Pointer res);

    /**
     * Translates getaddrinfo error codes to a human readable string, suitable for
     * error reporting.
     *
     * @param e
     *            Error code from getaddrinfo
     * @return A human-readable version of the error code
     */
    String gai_strerror(int e);

    /**
     * Rewinds the file pointer to the beginning of the utmp file. It is generally a
     * good idea to call it before any of the other functions.
     */
    void setutxent();

    /**
     * Closes the utmp file. It should be called when the user code is done
     * accessing the file with the other functions.
     */
    void endutxent();
}
