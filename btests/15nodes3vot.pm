dtmc

const int D;

module VotE
	v0: [0..20] init 0;
	[Et0_ok] v0 = 0 -> (v0' = 1);
	[Et0_fail] v0 = 0 -> (v0' = 2);

	[Et1_ok] v0 = 1 -> (v0' = 3);
	[Et1_fail] v0 = 1 -> (v0' = 4);

	[Et1_ok] v0 = 2 -> (v0' = 5);
	[Et1_fail] v0 = 2 -> (v0' = 6);

	[Et2_ok] v0 = 3 -> (v0' = 7);
	[Et2_fail] v0 = 3 -> (v0' = 8);

	[Et2_ok] v0 = 4 -> (v0' = 9);
	[Et2_fail] v0 = 4 -> (v0' = 10);

	[Et2_ok] v0 = 5 -> (v0' = 11);
	[Et2_fail] v0 = 5 -> (v0' = 12);

	[Et2_ok] v0 = 6 -> (v0' = 13);
	[Et2_fail] v0 = 6 -> (v0' = 14);

	[VotE_fail] v0 = 14 -> (v0' = 0);
	[VotE_fail] v0 = 13 -> (v0' = 0);
	[VotE_fail] v0 = 12 -> (v0' = 0);
	[VotE_ok] v0 = 11 -> (v0' = 0);
	[VotE_fail] v0 = 10 -> (v0' = 0);
	[VotE_ok] v0 = 9 -> (v0' = 0);
	[VotE_ok] v0 = 8 -> (v0' = 0);
	[VotE_ok] v0 = 7 -> (v0' = 0);
endmodule

module Et0
	r_0_0: [0..2] init 0;
	[Et0_run] r_0_0 = 0 ->  1 - 0.01 : (r_0_0' = 1) + 0.01 : (r_0_0' = 2);
	[Et0_ok] r_0_0 = 1 -> (r_0_0' = 0);
	[Et0_fail] r_0_0 = 2 -> (r_0_0' = 0);
endmodule

module Et1
	r_0_1: [0..2] init 0;
	[Et1_run] r_0_1 = 0 ->  1 - 0.01 : (r_0_1' = 1) + 0.01 : (r_0_1' = 2);
	[Et1_ok] r_0_1 = 1 -> (r_0_1' = 0);
	[Et1_fail] r_0_1 = 2 -> (r_0_1' = 0);
endmodule

module Et2
	r_0_2: [0..2] init 0;
	[Et2_run] r_0_2 = 0 ->  1 - 0.01 : (r_0_2' = 1) + 0.01 : (r_0_2' = 2);
	[Et2_ok] r_0_2 = 1 -> (r_0_2' = 0);
	[Et2_fail] r_0_2 = 2 -> (r_0_2' = 0);
endmodule

module VotH
	v1: [0..20] init 0;
	[Ht0_ok] v1 = 0 -> (v1' = 1);
	[Ht0_fail] v1 = 0 -> (v1' = 2);

	[Ht1_ok] v1 = 1 -> (v1' = 3);
	[Ht1_fail] v1 = 1 -> (v1' = 4);

	[Ht1_ok] v1 = 2 -> (v1' = 5);
	[Ht1_fail] v1 = 2 -> (v1' = 6);

	[Ht2_ok] v1 = 3 -> (v1' = 7);
	[Ht2_fail] v1 = 3 -> (v1' = 8);

	[Ht2_ok] v1 = 4 -> (v1' = 9);
	[Ht2_fail] v1 = 4 -> (v1' = 10);

	[Ht2_ok] v1 = 5 -> (v1' = 11);
	[Ht2_fail] v1 = 5 -> (v1' = 12);

	[Ht2_ok] v1 = 6 -> (v1' = 13);
	[Ht2_fail] v1 = 6 -> (v1' = 14);

	[VotH_fail] v1 = 14 -> (v1' = 0);
	[VotH_fail] v1 = 13 -> (v1' = 0);
	[VotH_fail] v1 = 12 -> (v1' = 0);
	[VotH_ok] v1 = 11 -> (v1' = 0);
	[VotH_fail] v1 = 10 -> (v1' = 0);
	[VotH_ok] v1 = 9 -> (v1' = 0);
	[VotH_ok] v1 = 8 -> (v1' = 0);
	[VotH_ok] v1 = 7 -> (v1' = 0);
endmodule

module Ht0
	r_1_0: [0..2] init 0;
	[Ht0_run] r_1_0 = 0 ->  1 - 0.01 : (r_1_0' = 1) + 0.01 : (r_1_0' = 2);
	[Ht0_ok] r_1_0 = 1 -> (r_1_0' = 0);
	[Ht0_fail] r_1_0 = 2 -> (r_1_0' = 0);
endmodule

module Ht1
	r_1_1: [0..2] init 0;
	[Ht1_run] r_1_1 = 0 ->  1 - 0.01 : (r_1_1' = 1) + 0.01 : (r_1_1' = 2);
	[Ht1_ok] r_1_1 = 1 -> (r_1_1' = 0);
	[Ht1_fail] r_1_1 = 2 -> (r_1_1' = 0);
endmodule

module Ht2
	r_1_2: [0..2] init 0;
	[Ht2_run] r_1_2 = 0 ->  1 - 0.01 : (r_1_2' = 1) + 0.01 : (r_1_2' = 2);
	[Ht2_ok] r_1_2 = 1 -> (r_1_2' = 0);
	[Ht2_fail] r_1_2 = 2 -> (r_1_2' = 0);
endmodule

module VotL
	v2: [0..20] init 0;
	[Lt0_ok] v2 = 0 -> (v2' = 1);
	[Lt0_fail] v2 = 0 -> (v2' = 2);

	[Lt1_ok] v2 = 1 -> (v2' = 3);
	[Lt1_fail] v2 = 1 -> (v2' = 4);

	[Lt1_ok] v2 = 2 -> (v2' = 5);
	[Lt1_fail] v2 = 2 -> (v2' = 6);

	[Lt2_ok] v2 = 3 -> (v2' = 7);
	[Lt2_fail] v2 = 3 -> (v2' = 8);

	[Lt2_ok] v2 = 4 -> (v2' = 9);
	[Lt2_fail] v2 = 4 -> (v2' = 10);

	[Lt2_ok] v2 = 5 -> (v2' = 11);
	[Lt2_fail] v2 = 5 -> (v2' = 12);

	[Lt2_ok] v2 = 6 -> (v2' = 13);
	[Lt2_fail] v2 = 6 -> (v2' = 14);

	[VotL_fail] v2 = 14 -> (v2' = 0);
	[VotL_fail] v2 = 13 -> (v2' = 0);
	[VotL_fail] v2 = 12 -> (v2' = 0);
	[VotL_ok] v2 = 11 -> (v2' = 0);
	[VotL_fail] v2 = 10 -> (v2' = 0);
	[VotL_ok] v2 = 9 -> (v2' = 0);
	[VotL_ok] v2 = 8 -> (v2' = 0);
	[VotL_ok] v2 = 7 -> (v2' = 0);
endmodule

module Lt0
	r_2_0: [0..2] init 0;
	[Lt0_run] r_2_0 = 0 ->  1 - 0.01 : (r_2_0' = 1) + 0.01 : (r_2_0' = 2);
	[Lt0_ok] r_2_0 = 1 -> (r_2_0' = 0);
	[Lt0_fail] r_2_0 = 2 -> (r_2_0' = 0);
endmodule

module Lt1
	r_2_1: [0..2] init 0;
	[Lt1_run] r_2_1 = 0 ->  1 - 0.01 : (r_2_1' = 1) + 0.01 : (r_2_1' = 2);
	[Lt1_ok] r_2_1 = 1 -> (r_2_1' = 0);
	[Lt1_fail] r_2_1 = 2 -> (r_2_1' = 0);
endmodule

module Lt2
	r_2_2: [0..2] init 0;
	[Lt2_run] r_2_2 = 0 ->  1 - 0.01 : (r_2_2' = 1) + 0.01 : (r_2_2' = 2);
	[Lt2_ok] r_2_2 = 1 -> (r_2_2' = 0);
	[Lt2_fail] r_2_2 = 2 -> (r_2_2' = 0);
endmodule

formula Gt = Gtbool;
formula Dt = Dtbool & Ctbool & Btbool;
formula Nt = Ntbool;
formula Jt = Jtbool;

module proc
	s : [0..26] init 0;
	Etbool: bool init false;
	Ctbool: bool init false;
	Btbool: bool init false;
	Dtbool: bool init false;
	Jtbool: bool init false;
	Ktbool: bool init false;
	Ltbool: bool init false;
	Ntbool: bool init false;
	Gtbool: bool init false;
	Htbool: bool init false;
	[Init_lo] s = 0 -> (s' = 2) & (Etbool' = false) & (Ktbool' = false) & (Ltbool' = false) & (Htbool' = false) & (Btbool' = false) & (Ctbool' = false) & (Ntbool' = false) & (Jtbool' = false) & (Gtbool' = false) & (Dtbool' = false);
	[Et0_run] s = 2 -> (s' = 7);
	[Kt_lo] s = 7 -> 1 - 0.01 : (s' = 13) & (Ktbool' = true) + 0.01: (s' =13);
	[VotE_ok] s = 13 -> (s' = 1);
	[VotE_fail] s = 13 -> (s' = 20);
	[At_lo] s = 1 -> 1 - 0.001 : (s' = 11) +0.001: (s' =18);
	[Lt0_run] s = 11 -> (s' = 17);
	[Ht0_run] s = 17 -> (s' = 4);
	[Bt_lo] s = 4 -> 1 - 0.01 : (s' = 16) & (Btbool' = true) + 0.01: (s' =16);
	[VotL_ok] s = 16 -> (s' = 15);
	[VotL_fail] s = 16 -> (s' = 22);
	[VotH_ok] s = 15 -> (s' = 10);
	[VotH_fail] s = 15 -> (s' = 21);
	[Ft_lo] s = 10 -> 1 - 0.001 : (s' = 3) +0.001: (s' =19);
	[Ct_lo] s = 3 -> 1 - 0.01 : (s' = 12) & (Ctbool' = true) + 0.01: (s' =12);
	[Nt_lo] s = 12 -> 1 - 0.01 : (s' = 6) & (Ntbool' = true) + 0.01: (s' =6);
	[Jt_lo] s = 6 -> 1 - 0.01 : (s' = 9) & (Jtbool' = true) + 0.01: (s' =9);
	[Mt_lo] s = 9 -> 1 - 0.001 : (s' = 8) +0.001: (s' =24);
	[It_lo] s = 8 -> 1 - 0.001 : (s' = 14) +0.001: (s' =23);
	[Gt_lo] s = 14 -> 1 - 0.01 : (s' = 5) & (Gtbool' = true) + 0.01: (s' =5);
	[Dt_lo] s = 5 -> 1 - 0.01 : (s' = 25) & (Dtbool' = true) + 0.01: (s' =25);
	[FinalLO0] s = 25 & Gt = false & Dt = false & Nt = false & Jt = false -> (s' = 0);
	[FinalLO1] s = 25 & Gt = true & Dt = true & Nt = false & Jt = false -> (s' = 0);
	[FinalLO2] s = 25 & Gt = true & Dt = true & Nt = true & Jt = true -> (s' = 0);
	[FinalLO3] s = 25 & Dt = true & Nt = true & Jt = true & Gt = false -> (s' = 0);
	[FinalLO4] s = 25 & Nt = true & Jt = true & Gt = false & Dt = false -> (s' = 0);
	[FinalLO5] s = 25 & Gt = true & Dt = false & Nt = false & Jt = false -> (s' = 0);
	[FinalLO6] s = 25 & Dt = true & Gt = false & Nt = false & Jt = false -> (s' = 0);
	[FinalLO7] s = 25 & Gt = true & Nt = true & Jt = true & Dt = false -> (s' = 0);
	[FinalLO8] s = 25 & Gt = true & Dt = true & Jt = true & Nt = false -> (s' = 0);
	[FinalLO9] s = 25 & Nt = true & Gt = false & Dt = false & Jt = false -> (s' = 0);
	[FinalLO10] s = 25 & Gt = true & Nt = true & Dt = false & Jt = false -> (s' = 0);
	[FinalLO11] s = 25 & Gt = true & Dt = true & Nt = true & Jt = false -> (s' = 0);
	[FinalLO12] s = 25 & Dt = true & Jt = true & Gt = false & Nt = false -> (s' = 0);
	[FinalLO13] s = 25 & Dt = true & Nt = true & Gt = false & Jt = false -> (s' = 0);
	[FinalLO14] s = 25 & Jt = true & Gt = false & Dt = false & Nt = false -> (s' = 0);
	[FinalLO15] s = 25 & Gt = true & Jt = true & Dt = false & Nt = false -> (s' = 0);

	[VotL_hi] s = 22 -> (s' =20);
	[VotE_hi] s = 20 -> (s' =21);
	[VotH_hi] s = 21 -> (s' =23);
	[It_hi] s = 23 -> (s' =24);
	[Mt_hi] s = 24 -> (s' =19);
	[Ft_hi] s = 19 -> (s' =18);
	[At_hi] s = 18 -> (s' =0);
endmodule

rewards "Nt_cycles"
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
	[FinalLO7] true : 1;
	[FinalLO9] true : 1;
	[FinalLO10] true : 1;
	[FinalLO11] true : 1;
	[FinalLO13] true : 1;
endrewards

rewards "Dt_cycles"
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO6] true : 1;
	[FinalLO8] true : 1;
	[FinalLO11] true : 1;
	[FinalLO12] true : 1;
	[FinalLO13] true : 1;
endrewards

rewards "Jt_cycles"
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO12] true : 1;
	[FinalLO14] true : 1;
	[FinalLO15] true : 1;
endrewards

rewards "Gt_cycles"
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO5] true : 1;
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO10] true : 1;
	[FinalLO11] true : 1;
	[FinalLO15] true : 1;
endrewards

rewards "total_cycles"
	[FinalLO0] true : 1;
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO9] true : 1;
	[FinalLO10] true : 1;
	[FinalLO11] true : 1;
	[FinalLO12] true : 1;
	[FinalLO13] true : 1;
	[FinalLO14] true : 1;
	[FinalLO15] true : 1;
	[At_hi] true : 1;
endrewards

