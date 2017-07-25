dtmc

const int D;

module VotA
	v: [0..20] init 0;
	[Avoid0_ok] v = 0 -> (v' = 1);
	[Avoid0_fail] v = 0 -> (v' = 2);

	[Avoid1_ok] v = 1 -> (v' = 3);
	[Avoid1_fail] v = 1 -> (v' = 4);

	[Avoid1_ok] v = 2 -> (v' = 5);
	[Avoid1_fail] v = 2 -> (v' = 6);

	[Avoid2_ok] v = 3 -> (v' = 7);
	[Avoid2_fail] v = 3 -> (v' = 8);

	[Avoid2_ok] v = 4 -> (v' = 9);
	[Avoid2_fail] v = 4 -> (v' = 10);

	[Avoid2_ok] v = 5 -> (v' = 11);
	[Avoid2_fail] v = 5 -> (v' = 12);

	[Avoid2_ok] v = 6 -> (v' = 13);
	[Avoid2_fail] v = 6 -> (v' = 14);

	[VotA_fail] v = 14 -> (v' = 0);
	[VotA_fail] v = 13 -> (v' = 0);
	[VotA_fail] v = 12 -> (v' = 0);
	[VotA_ok] v = 11 -> (v' = 0);
	[VotA_fail] v = 10 -> (v' = 0);
	[VotA_ok] v = 9 -> (v' = 0);
	[VotA_ok] v = 8 -> (v' = 0);
	[VotA_ok] v = 7 -> (v' = 0);
endmodule

module Avoid0
	v0: [0..2] init 0;
	[Avoid0_run] v0 = 0 ->  1 - 0.01 : (v0' = 1) + 0.01 : (v0' = 2);
	[Avoid0_ok] v0 = 1 -> (v0' = 0);
	[Avoid0_fail] v0 = 2 -> (v0' = 0);
endmodule

module Avoid1
	v1: [0..2] init 0;
	[Avoid1_run] v1 = 0 ->  1 - 0.01 : (v1' = 1) + 0.01 : (v1' = 2);
	[Avoid1_ok] v1 = 1 -> (v1' = 0);
	[Avoid1_fail] v1 = 2 -> (v1' = 0);
endmodule

module Avoid2
	v2: [0..2] init 0;
	[Avoid2_run] v2 = 0 ->  1 - 0.01 : (v2' = 1) + 0.01 : (v2' = 2);
	[Avoid2_ok] v2 = 1 -> (v2' = 0);
	[Avoid2_fail] v2 = 2 -> (v2' = 0);
endmodule

formula Rec = Recbool & GPSbool;
formula Video = Videobool;
formula Shar = Sharbool & Logbool;

module proc
	s : [0..14] init 0;
	Avoidbool: bool init false;
	Videobool: bool init false;
	GPSbool: bool init false;
	Recbool: bool init false;
	Logbool: bool init false;
	Sharbool: bool init false;
	[Init_lo] s = 0 -> (s' = 8) & (Avoidbool' = false) & (Videobool' = false) & (GPSbool' = false) & (Recbool' = false) & (Logbool' = false) & (Sharbool' = false);
	[Avoid0_run] s = 8 -> (s' = 2);
	[VotA_ok] s = 2 -> (s' = 9);
	[VotA_fail] s = 2 -> (s' = 10);
	[Video_lo] s = 9 -> 1 - 0.01 : (s' = 4) & (Videobool' = true) + 0.01: (s' =4);
	[GPS_lo] s = 4 -> 1 - 0.01 : (s' = 5) & (GPSbool' = true) + 0.01: (s' =5);
	[Nav_lo] s = 5 -> 1 - 0.001 : (s' = 6) +0.001: (s' =11);
	[Rec_lo] s = 6 -> 1 - 0.01 : (s' = 7) & (Recbool' = true) + 0.01: (s' =7);
	[Stab_lo] s = 7 -> 1 - 0.001 : (s' = 1) +0.001: (s' =12);
	[Log_lo] s = 1 -> 1 - 0.01 : (s' = 3) & (Logbool' = true) + 0.01: (s' =3);
	[Shar_lo] s = 3 -> 1 - 0.01 : (s' = 13) & (Sharbool' = true) + 0.01: (s' =13);
	[FinalLO0] s = 13 & Rec = false & Video = false & Shar = false -> (s' = 0);
	[FinalLO1] s = 13 & Rec = true & Video = true & Shar = false -> (s' = 0);
	[FinalLO2] s = 13 & Rec = true & Video = false & Shar = false -> (s' = 0);
	[FinalLO3] s = 13 & Video = true & Rec = false & Shar = false -> (s' = 0);
	[FinalLO4] s = 13 & Rec = true & Shar = true & Video = false -> (s' = 0);
	[FinalLO5] s = 13 & Shar = true & Rec = false & Video = false -> (s' = 0);
	[FinalLO6] s = 13 & Rec = true & Video = true & Shar = true -> (s' = 0);
	[FinalLO7] s = 13 & Video = true & Shar = true & Rec = false -> (s' = 0);

	[VotA_hi] s = 10 -> (s' =11);
	[Nav_hi] s = 11 -> (s' =12);
	[Stab_hi] s = 12 -> (s' =0);
endmodule

rewards "Shar_cycles"
	[FinalLO4] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
endrewards

rewards "Rec_cycles"
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO4] true : 1;
	[FinalLO6] true : 1;
endrewards

rewards "Video_cycles"
	[FinalLO1] true : 1;
	[FinalLO3] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
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
	[Stab_hi] true : 1;
endrewards

