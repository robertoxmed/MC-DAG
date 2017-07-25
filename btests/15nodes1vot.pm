dtmc

const int D;

module VotE
	v: [0..20] init 0;
	[Et0_ok] v = 0 -> (v' = 1);
	[Et0_fail] v = 0 -> (v' = 2);

	[Et1_ok] v = 1 -> (v' = 3);
	[Et1_fail] v = 1 -> (v' = 4);

	[Et1_ok] v = 2 -> (v' = 5);
	[Et1_fail] v = 2 -> (v' = 6);

	[Et2_ok] v = 3 -> (v' = 7);
	[Et2_fail] v = 3 -> (v' = 8);

	[Et2_ok] v = 4 -> (v' = 9);
	[Et2_fail] v = 4 -> (v' = 10);

	[Et2_ok] v = 5 -> (v' = 11);
	[Et2_fail] v = 5 -> (v' = 12);

	[Et2_ok] v = 6 -> (v' = 13);
	[Et2_fail] v = 6 -> (v' = 14);

	[VotE_fail] v = 14 -> (v' = 0);
	[VotE_fail] v = 13 -> (v' = 0);
	[VotE_fail] v = 12 -> (v' = 0);
	[VotE_ok] v = 11 -> (v' = 0);
	[VotE_fail] v = 10 -> (v' = 0);
	[VotE_ok] v = 9 -> (v' = 0);
	[VotE_ok] v = 8 -> (v' = 0);
	[VotE_ok] v = 7 -> (v' = 0);
endmodule

module Et0
	v0: [0..2] init 0;
	[Et0_run] v0 = 0 ->  1 - 0.01 : (v0' = 1) + 0.01 : (v0' = 2);
	[Et0_ok] v0 = 1 -> (v0' = 0);
	[Et0_fail] v0 = 2 -> (v0' = 0);
endmodule

module Et1
	v1: [0..2] init 0;
	[Et1_run] v1 = 0 ->  1 - 0.01 : (v1' = 1) + 0.01 : (v1' = 2);
	[Et1_ok] v1 = 1 -> (v1' = 0);
	[Et1_fail] v1 = 2 -> (v1' = 0);
endmodule

module Et2
	v2: [0..2] init 0;
	[Et2_run] v2 = 0 ->  1 - 0.01 : (v2' = 1) + 0.01 : (v2' = 2);
	[Et2_ok] v2 = 1 -> (v2' = 0);
	[Et2_fail] v2 = 2 -> (v2' = 0);
endmodule

formula Gt = Gtbool;
formula Dt = Dtbool & Ctbool & Btbool;
formula Nt = Ntbool;
formula It = Itbool & Htbool;
formula Mt = Mtbool & Ktbool & Ltbool;
formula Jt = Jtbool;

module proc
	s : [0..20] init 0;
	Etbool: bool init false;
	Ktbool: bool init false;
	Ltbool: bool init false;
	Btbool: bool init false;
	Htbool: bool init false;
	Ntbool: bool init false;
	Ctbool: bool init false;
	Mtbool: bool init false;
	Jtbool: bool init false;
	Gtbool: bool init false;
	Itbool: bool init false;
	Dtbool: bool init false;
	[Init_lo] s = 0 -> (s' = 4) & (Etbool' = false) & (Ktbool' = false) & (Ltbool' = false) & (Btbool' = false) & (Htbool' = false) & (Ntbool' = false) & (Ctbool' = false) & (Mtbool' = false) & (Jtbool' = false) & (Gtbool' = false) & (Itbool' = false) & (Dtbool' = false);
	[Et0_run] s = 4 -> (s' = 12);
	[At_lo] s = 10 -> 1 - 0.001 : (s' = 15) +0.001: (s' =18);
	[VotE_ok] s = 12 -> (s' = 10);
	[VotE_fail] s = 12 -> (s' = 16);
	[Kt_lo] s = 15 -> 1 - 0.01 : (s' = 8) & (Ktbool' = true) + 0.01: (s' =8);
	[Lt_lo] s = 8 -> 1 - 0.01 : (s' = 14) & (Ltbool' = true) + 0.01: (s' =14);
	[Bt_lo] s = 14 -> 1 - 0.01 : (s' = 3) & (Btbool' = true) + 0.01: (s' =3);
	[Ft_lo] s = 3 -> 1 - 0.001 : (s' = 2) +0.001: (s' =17);
	[Ht_lo] s = 2 -> 1 - 0.01 : (s' = 7) & (Htbool' = true) + 0.01: (s' =7);
	[Nt_lo] s = 7 -> 1 - 0.01 : (s' = 11) & (Ntbool' = true) + 0.01: (s' =11);
	[Ct_lo] s = 11 -> 1 - 0.01 : (s' = 6) & (Ctbool' = true) + 0.01: (s' =6);
	[Mt_lo] s = 6 -> 1 - 0.01 : (s' = 13) & (Mtbool' = true) + 0.01: (s' =13);
	[Jt_lo] s = 13 -> 1 - 0.01 : (s' = 5) & (Jtbool' = true) + 0.01: (s' =5);
	[Gt_lo] s = 5 -> 1 - 0.01 : (s' = 9) & (Gtbool' = true) + 0.01: (s' =9);
	[It_lo] s = 9 -> 1 - 0.01 : (s' = 1) & (Itbool' = true) + 0.01: (s' =1);
	[Dt_lo] s = 1 -> 1 - 0.01 : (s' = 19) & (Dtbool' = true) + 0.01: (s' =19);
	[FinalLO0] s = 19 & Gt = false & Dt = false & Nt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO1] s = 19 & Gt = true & Mt = true & Dt = false & Nt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO2] s = 19 & Nt = true & It = true & Jt = true & Gt = false & Dt = false & Mt = false -> (s' = 0);
	[FinalLO3] s = 19 & Gt = true & It = true & Dt = false & Nt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO4] s = 19 & Nt = true & Mt = true & Jt = true & Gt = false & Dt = false & It = false -> (s' = 0);
	[FinalLO5] s = 19 & Gt = true & Jt = true & Dt = false & Nt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO6] s = 19 & Nt = true & It = true & Mt = true & Gt = false & Dt = false & Jt = false -> (s' = 0);
	[FinalLO7] s = 19 & Gt = true & Dt = true & It = true & Mt = true & Nt = false & Jt = false -> (s' = 0);
	[FinalLO8] s = 19 & Dt = true & Nt = true & It = true & Mt = true & Jt = true & Gt = false -> (s' = 0);
	[FinalLO9] s = 19 & Gt = true & Dt = true & It = true & Jt = true & Nt = false & Mt = false -> (s' = 0);
	[FinalLO10] s = 19 & Dt = true & Mt = true & Gt = false & Nt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO11] s = 19 & Gt = true & Dt = true & Nt = true & Mt = true & It = false & Jt = false -> (s' = 0);
	[FinalLO12] s = 19 & Dt = true & It = true & Gt = false & Nt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO13] s = 19 & Gt = true & Dt = true & Nt = true & It = true & Mt = false & Jt = false -> (s' = 0);
	[FinalLO14] s = 19 & Dt = true & Nt = true & Mt = true & Jt = true & Gt = false & It = false -> (s' = 0);
	[FinalLO15] s = 19 & It = true & Mt = true & Jt = true & Gt = false & Dt = false & Nt = false -> (s' = 0);
	[FinalLO16] s = 19 & Gt = true & Nt = true & It = true & Mt = true & Jt = true & Dt = false -> (s' = 0);
	[FinalLO17] s = 19 & Dt = true & Nt = true & It = true & Jt = true & Gt = false & Mt = false -> (s' = 0);
	[FinalLO18] s = 19 & Gt = true & Dt = true & Nt = true & It = true & Mt = true & Jt = true -> (s' = 0);
	[FinalLO19] s = 19 & Gt = true & Nt = true & It = true & Dt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO20] s = 19 & Dt = true & Gt = false & Nt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO21] s = 19 & Nt = true & Gt = false & Dt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO22] s = 19 & Gt = true & It = true & Mt = true & Jt = true & Dt = false & Nt = false -> (s' = 0);
	[FinalLO23] s = 19 & Gt = true & Dt = true & Mt = true & Jt = true & Nt = false & It = false -> (s' = 0);
	[FinalLO24] s = 19 & Dt = true & Nt = true & It = true & Gt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO25] s = 19 & Gt = true & Dt = true & Nt = true & Jt = true & It = false & Mt = false -> (s' = 0);
	[FinalLO26] s = 19 & Dt = true & Jt = true & Gt = false & Nt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO27] s = 19 & Dt = true & It = true & Mt = true & Jt = true & Gt = false & Nt = false -> (s' = 0);
	[FinalLO28] s = 19 & It = true & Gt = false & Dt = false & Nt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO29] s = 19 & Dt = true & Nt = true & Mt = true & Gt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO30] s = 19 & Mt = true & Gt = false & Dt = false & Nt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO31] s = 19 & Gt = true & Nt = true & Mt = true & Dt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO32] s = 19 & Dt = true & It = true & Jt = true & Gt = false & Nt = false & Mt = false -> (s' = 0);
	[FinalLO33] s = 19 & Dt = true & Mt = true & Jt = true & Gt = false & Nt = false & It = false -> (s' = 0);
	[FinalLO34] s = 19 & Gt = true & Dt = true & Nt = true & Mt = true & Jt = true & It = false -> (s' = 0);
	[FinalLO35] s = 19 & Gt = true & Dt = true & Nt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO36] s = 19 & Nt = true & It = true & Gt = false & Dt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO37] s = 19 & Gt = true & Dt = true & It = true & Mt = true & Jt = true & Nt = false -> (s' = 0);
	[FinalLO38] s = 19 & Gt = true & Dt = true & Nt = true & It = true & Mt = true & Jt = false -> (s' = 0);
	[FinalLO39] s = 19 & Gt = true & Nt = true & It = true & Jt = true & Dt = false & Mt = false -> (s' = 0);
	[FinalLO40] s = 19 & Gt = true & Dt = false & Nt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO41] s = 19 & Nt = true & Mt = true & Gt = false & Dt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO42] s = 19 & Gt = true & It = true & Mt = true & Dt = false & Nt = false & Jt = false -> (s' = 0);
	[FinalLO43] s = 19 & Dt = true & It = true & Mt = true & Gt = false & Nt = false & Jt = false -> (s' = 0);
	[FinalLO44] s = 19 & It = true & Jt = true & Gt = false & Dt = false & Nt = false & Mt = false -> (s' = 0);
	[FinalLO45] s = 19 & Gt = true & Nt = true & Jt = true & Dt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO46] s = 19 & Dt = true & Nt = true & Jt = true & Gt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO47] s = 19 & Gt = true & Dt = true & Nt = true & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO48] s = 19 & Gt = true & Nt = true & Mt = true & Jt = true & Dt = false & It = false -> (s' = 0);
	[FinalLO49] s = 19 & Jt = true & Gt = false & Dt = false & Nt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO50] s = 19 & Mt = true & Jt = true & Gt = false & Dt = false & Nt = false & It = false -> (s' = 0);
	[FinalLO51] s = 19 & Gt = true & Dt = true & It = true & Nt = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO52] s = 19 & Gt = true & Dt = true & Jt = true & Nt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO53] s = 19 & Gt = true & Dt = true & Mt = true & Nt = false & It = false & Jt = false -> (s' = 0);
	[FinalLO54] s = 19 & Gt = true & Mt = true & Jt = true & Dt = false & Nt = false & It = false -> (s' = 0);
	[FinalLO55] s = 19 & Gt = true & It = true & Jt = true & Dt = false & Nt = false & Mt = false -> (s' = 0);
	[FinalLO56] s = 19 & Nt = true & Jt = true & Gt = false & Dt = false & It = false & Mt = false -> (s' = 0);
	[FinalLO57] s = 19 & Nt = true & It = true & Mt = true & Jt = true & Gt = false & Dt = false -> (s' = 0);
	[FinalLO58] s = 19 & Gt = true & Nt = true & Dt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO59] s = 19 & Dt = true & Nt = true & Gt = false & It = false & Mt = false & Jt = false -> (s' = 0);
	[FinalLO60] s = 19 & Dt = true & Nt = true & It = true & Mt = true & Gt = false & Jt = false -> (s' = 0);
	[FinalLO61] s = 19 & Gt = true & Nt = true & It = true & Mt = true & Dt = false & Jt = false -> (s' = 0);
	[FinalLO62] s = 19 & Gt = true & Dt = true & Nt = true & It = true & Jt = true & Mt = false -> (s' = 0);
	[FinalLO63] s = 19 & It = true & Mt = true & Gt = false & Dt = false & Nt = false & Jt = false -> (s' = 0);

	[VotE_hi] s = 16 -> (s' =18);
	[At_hi] s = 18 -> (s' =17);
	[Ft_hi] s = 17 -> (s' =0);
endmodule

rewards "Dt_cycles"
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO9] true : 1;
	[FinalLO10] true : 1;
	[FinalLO11] true : 1;
	[FinalLO12] true : 1;
	[FinalLO13] true : 1;
	[FinalLO14] true : 1;
	[FinalLO17] true : 1;
	[FinalLO18] true : 1;
	[FinalLO20] true : 1;
	[FinalLO23] true : 1;
	[FinalLO24] true : 1;
	[FinalLO25] true : 1;
	[FinalLO26] true : 1;
	[FinalLO27] true : 1;
	[FinalLO29] true : 1;
	[FinalLO32] true : 1;
	[FinalLO33] true : 1;
	[FinalLO34] true : 1;
	[FinalLO35] true : 1;
	[FinalLO37] true : 1;
	[FinalLO38] true : 1;
	[FinalLO43] true : 1;
	[FinalLO46] true : 1;
	[FinalLO47] true : 1;
	[FinalLO51] true : 1;
	[FinalLO52] true : 1;
	[FinalLO53] true : 1;
	[FinalLO59] true : 1;
	[FinalLO60] true : 1;
	[FinalLO62] true : 1;
endrewards

rewards "It_cycles"
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO9] true : 1;
	[FinalLO12] true : 1;
	[FinalLO13] true : 1;
	[FinalLO15] true : 1;
	[FinalLO16] true : 1;
	[FinalLO17] true : 1;
	[FinalLO18] true : 1;
	[FinalLO19] true : 1;
	[FinalLO22] true : 1;
	[FinalLO24] true : 1;
	[FinalLO27] true : 1;
	[FinalLO28] true : 1;
	[FinalLO32] true : 1;
	[FinalLO36] true : 1;
	[FinalLO37] true : 1;
	[FinalLO38] true : 1;
	[FinalLO39] true : 1;
	[FinalLO42] true : 1;
	[FinalLO43] true : 1;
	[FinalLO44] true : 1;
	[FinalLO51] true : 1;
	[FinalLO55] true : 1;
	[FinalLO57] true : 1;
	[FinalLO60] true : 1;
	[FinalLO61] true : 1;
	[FinalLO62] true : 1;
	[FinalLO63] true : 1;
endrewards

rewards "Gt_cycles"
	[FinalLO1] true : 1;
	[FinalLO3] true : 1;
	[FinalLO5] true : 1;
	[FinalLO7] true : 1;
	[FinalLO9] true : 1;
	[FinalLO11] true : 1;
	[FinalLO13] true : 1;
	[FinalLO16] true : 1;
	[FinalLO18] true : 1;
	[FinalLO19] true : 1;
	[FinalLO22] true : 1;
	[FinalLO23] true : 1;
	[FinalLO25] true : 1;
	[FinalLO31] true : 1;
	[FinalLO34] true : 1;
	[FinalLO35] true : 1;
	[FinalLO37] true : 1;
	[FinalLO38] true : 1;
	[FinalLO39] true : 1;
	[FinalLO40] true : 1;
	[FinalLO42] true : 1;
	[FinalLO45] true : 1;
	[FinalLO47] true : 1;
	[FinalLO48] true : 1;
	[FinalLO51] true : 1;
	[FinalLO52] true : 1;
	[FinalLO53] true : 1;
	[FinalLO54] true : 1;
	[FinalLO55] true : 1;
	[FinalLO58] true : 1;
	[FinalLO61] true : 1;
	[FinalLO62] true : 1;
endrewards

rewards "Mt_cycles"
	[FinalLO1] true : 1;
	[FinalLO4] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
	[FinalLO8] true : 1;
	[FinalLO10] true : 1;
	[FinalLO11] true : 1;
	[FinalLO14] true : 1;
	[FinalLO15] true : 1;
	[FinalLO16] true : 1;
	[FinalLO18] true : 1;
	[FinalLO22] true : 1;
	[FinalLO23] true : 1;
	[FinalLO27] true : 1;
	[FinalLO29] true : 1;
	[FinalLO30] true : 1;
	[FinalLO31] true : 1;
	[FinalLO33] true : 1;
	[FinalLO34] true : 1;
	[FinalLO37] true : 1;
	[FinalLO38] true : 1;
	[FinalLO41] true : 1;
	[FinalLO42] true : 1;
	[FinalLO43] true : 1;
	[FinalLO48] true : 1;
	[FinalLO50] true : 1;
	[FinalLO53] true : 1;
	[FinalLO54] true : 1;
	[FinalLO57] true : 1;
	[FinalLO60] true : 1;
	[FinalLO61] true : 1;
	[FinalLO63] true : 1;
endrewards

rewards "Nt_cycles"
	[FinalLO2] true : 1;
	[FinalLO4] true : 1;
	[FinalLO6] true : 1;
	[FinalLO8] true : 1;
	[FinalLO11] true : 1;
	[FinalLO13] true : 1;
	[FinalLO14] true : 1;
	[FinalLO16] true : 1;
	[FinalLO17] true : 1;
	[FinalLO18] true : 1;
	[FinalLO19] true : 1;
	[FinalLO21] true : 1;
	[FinalLO24] true : 1;
	[FinalLO25] true : 1;
	[FinalLO29] true : 1;
	[FinalLO31] true : 1;
	[FinalLO34] true : 1;
	[FinalLO36] true : 1;
	[FinalLO38] true : 1;
	[FinalLO39] true : 1;
	[FinalLO41] true : 1;
	[FinalLO45] true : 1;
	[FinalLO46] true : 1;
	[FinalLO47] true : 1;
	[FinalLO48] true : 1;
	[FinalLO56] true : 1;
	[FinalLO57] true : 1;
	[FinalLO58] true : 1;
	[FinalLO59] true : 1;
	[FinalLO60] true : 1;
	[FinalLO61] true : 1;
	[FinalLO62] true : 1;
endrewards

rewards "Jt_cycles"
	[FinalLO2] true : 1;
	[FinalLO4] true : 1;
	[FinalLO5] true : 1;
	[FinalLO8] true : 1;
	[FinalLO9] true : 1;
	[FinalLO14] true : 1;
	[FinalLO15] true : 1;
	[FinalLO16] true : 1;
	[FinalLO17] true : 1;
	[FinalLO18] true : 1;
	[FinalLO22] true : 1;
	[FinalLO23] true : 1;
	[FinalLO25] true : 1;
	[FinalLO26] true : 1;
	[FinalLO27] true : 1;
	[FinalLO32] true : 1;
	[FinalLO33] true : 1;
	[FinalLO34] true : 1;
	[FinalLO37] true : 1;
	[FinalLO39] true : 1;
	[FinalLO44] true : 1;
	[FinalLO45] true : 1;
	[FinalLO46] true : 1;
	[FinalLO48] true : 1;
	[FinalLO49] true : 1;
	[FinalLO50] true : 1;
	[FinalLO52] true : 1;
	[FinalLO54] true : 1;
	[FinalLO55] true : 1;
	[FinalLO56] true : 1;
	[FinalLO57] true : 1;
	[FinalLO62] true : 1;
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
	[FinalLO16] true : 1;
	[FinalLO17] true : 1;
	[FinalLO18] true : 1;
	[FinalLO19] true : 1;
	[FinalLO20] true : 1;
	[FinalLO21] true : 1;
	[FinalLO22] true : 1;
	[FinalLO23] true : 1;
	[FinalLO24] true : 1;
	[FinalLO25] true : 1;
	[FinalLO26] true : 1;
	[FinalLO27] true : 1;
	[FinalLO28] true : 1;
	[FinalLO29] true : 1;
	[FinalLO30] true : 1;
	[FinalLO31] true : 1;
	[FinalLO32] true : 1;
	[FinalLO33] true : 1;
	[FinalLO34] true : 1;
	[FinalLO35] true : 1;
	[FinalLO36] true : 1;
	[FinalLO37] true : 1;
	[FinalLO38] true : 1;
	[FinalLO39] true : 1;
	[FinalLO40] true : 1;
	[FinalLO41] true : 1;
	[FinalLO42] true : 1;
	[FinalLO43] true : 1;
	[FinalLO44] true : 1;
	[FinalLO45] true : 1;
	[FinalLO46] true : 1;
	[FinalLO47] true : 1;
	[FinalLO48] true : 1;
	[FinalLO49] true : 1;
	[FinalLO50] true : 1;
	[FinalLO51] true : 1;
	[FinalLO52] true : 1;
	[FinalLO53] true : 1;
	[FinalLO54] true : 1;
	[FinalLO55] true : 1;
	[FinalLO56] true : 1;
	[FinalLO57] true : 1;
	[FinalLO58] true : 1;
	[FinalLO59] true : 1;
	[FinalLO60] true : 1;
	[FinalLO61] true : 1;
	[FinalLO62] true : 1;
	[FinalLO63] true : 1;
	[Ft_hi] true : 1;
endrewards

