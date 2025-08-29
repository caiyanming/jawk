package org.metricshub.jawk.intermediate;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Jawk
 * ჻჻჻჻჻჻
 * Copyright (C) 2006 - 2025 MetricsHub
 * ჻჻჻჻჻჻
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.util.Map;
import java.util.HashMap;

public enum Opcode {
	POP(257),
	PUSH(258),
	IFFALSE(259),
	TO_NUMBER(260),
	IFTRUE(261),
	GOTO(262),
	NOP(263),
	PRINT(264),
	PRINT_TO_FILE(265),
	PRINT_TO_PIPE(266),
	PRINTF(267),
	PRINTF_TO_FILE(268),
	PRINTF_TO_PIPE(269),
	SPRINTF(270),
	LENGTH(271),
	CONCAT(272),
	ASSIGN(273),
	ASSIGN_ARRAY(274),
	ASSIGN_AS_INPUT(275),
	ASSIGN_AS_INPUT_FIELD(276),
	DEREFERENCE(277),
	PLUS_EQ(278),
	MINUS_EQ(279),
	MULT_EQ(280),
	DIV_EQ(281),
	MOD_EQ(282),
	POW_EQ(283),
	PLUS_EQ_ARRAY(284),
	MINUS_EQ_ARRAY(285),
	MULT_EQ_ARRAY(286),
	DIV_EQ_ARRAY(287),
	MOD_EQ_ARRAY(288),
	POW_EQ_ARRAY(289),
	PLUS_EQ_INPUT_FIELD(290),
	MINUS_EQ_INPUT_FIELD(291),
	MULT_EQ_INPUT_FIELD(292),
	DIV_EQ_INPUT_FIELD(293),
	MOD_EQ_INPUT_FIELD(294),
	POW_EQ_INPUT_FIELD(295),
	SRAND(296),
	RAND(297),
	INTFUNC(298),
	SQRT(299),
	LOG(300),
	EXP(301),
	SIN(302),
	COS(303),
	ATAN2(304),
	MATCH(305),
	INDEX(306),
	SUB_FOR_DOLLAR_0(307),
	SUB_FOR_DOLLAR_REFERENCE(308),
	SUB_FOR_VARIABLE(309),
	SUB_FOR_ARRAY_REFERENCE(310),
	SPLIT(311),
	SUBSTR(312),
	TOLOWER(313),
	TOUPPER(314),
	SYSTEM(315),
	SWAP(316),
	ADD(317),
	SUBTRACT(318),
	MULTIPLY(319),
	DIVIDE(320),
	MOD(321),
	POW(322),
	INC(323),
	DEC(324),
	INC_ARRAY_REF(325),
	DEC_ARRAY_REF(326),
	INC_DOLLAR_REF(327),
	DEC_DOLLAR_REF(328),
	DUP(329),
	NOT(330),
	NEGATE(331),
	CMP_EQ(332),
	CMP_LT(333),
	CMP_GT(334),
	MATCHES(335),
	SLEEP(336),
	DUMP(337),
	DEREF_ARRAY(338),
	KEYLIST(339),
	IS_EMPTY_KEYLIST(340),
	GET_FIRST_AND_REMOVE_FROM_KEYLIST(341),
	CHECK_CLASS(342),
	GET_INPUT_FIELD(343),
	CONSUME_INPUT(344),
	GETLINE_INPUT(345),
	USE_AS_FILE_INPUT(346),
	USE_AS_COMMAND_INPUT(347),
	NF_OFFSET(348),
	NR_OFFSET(349),
	FNR_OFFSET(350),
	FS_OFFSET(351),
	RS_OFFSET(352),
	OFS_OFFSET(353),
	RSTART_OFFSET(354),
	RLENGTH_OFFSET(355),
	FILENAME_OFFSET(356),
	SUBSEP_OFFSET(357),
	CONVFMT_OFFSET(358),
	OFMT_OFFSET(359),
	ENVIRON_OFFSET(360),
	ARGC_OFFSET(361),
	ARGV_OFFSET(362),
	APPLY_RS(363),
	CALL_FUNCTION(364),
	FUNCTION(365),
	SET_RETURN_RESULT(366),
	RETURN_FROM_FUNCTION(367),
	SET_NUM_GLOBALS(368),
	CLOSE(369),
	APPLY_SUBSEP(370),
	DELETE_ARRAY_ELEMENT(371),
	SET_EXIT_ADDRESS(372),
	SET_WITHIN_END_BLOCKS(373),
	EXIT_WITH_CODE(374),
	REGEXP(375),
	CONDITION_PAIR(376),
	IS_IN(377),
	CAST_INT(378),
	CAST_DOUBLE(379),
	CAST_STRING(380),
	THIS(381),
	EXTENSION(382),
	EXEC(383),
	DELETE_ARRAY(384),
	UNARY_PLUS(385),
	EXIT_WITHOUT_CODE(386),
	ORS_OFFSET(387),
	POSTINC(388),
	POSTDEC(389),
	SET_INPUT_FOR_EVAL(390),
	;

	private final int id;
	private static final Map<Integer, Opcode> ID_MAP = new HashMap<>();

	static {
		for (Opcode op : values()) {
			ID_MAP.put(op.id, op);
		}
	}

	Opcode(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static Opcode fromId(int id) {
		Opcode op = ID_MAP.get(id);
		if (op == null) {
			throw new IllegalArgumentException("Unknown opcode: " + id);
		}
		return op;
	}
}
