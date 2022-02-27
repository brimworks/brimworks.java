package com.brimworks.serde.json;

import java.nio.ByteBuffer;
import com.brimworks.serde.SerdeEvent;

abstract class JsonByteReaderBase {
    // State that is built.
    protected JsonNumber numberParser;
    protected StringParser stringParser;
    protected boolean booleanValue;

    protected int mark, pline, line;
    protected ByteBuffer input;
    // Ragel writes this state:
    protected int cs, p, pe, ts, te, act, eof;

    // Read methods:
    abstract protected byte[] readBytes(int start, int end);
    abstract protected ByteBuffer readByteBuffer(int start, int end);

    // Handlers:
    abstract protected boolean handleComma();
    abstract protected boolean handleColon();
    abstract protected void handleUnexpectedChar();

    %% machine json;
    %% alphtype int;
    %% getkey input.get(p);
    %% write data;
    protected void init() {
        %% write init;
        numberParser.reset();
        stringParser.reset();
        eof = -1;
        mark = -1;
        line = 1;
        pline = 0;
    }

    public SerdeEvent exec() {
        SerdeEvent event = SerdeEvent.UNDERFLOW;
        %% write exec;
        return event;
    }
}

%%{

LF = 0x0A;
CR = 0x0D;
LS = 0xE2 0x80 0xA8;
PS = 0xE2 0x80 0xA9;
TAB = 0x09;
VT = 0x0B;
FF = 0x0C;
SP = 0x20;
SBSP = 0xC2 0xA0;
BOM = 0xEF 0xBB 0xBF;
NBSP = 0xC2 0xA0;
U_Zs =
    0x20 |
    0xc2 0xa0 |
    0xe1 0x9a 0x80 |
    0xe2 0x80 0x80 |
    0xe2 0x80 0x81 |
    0xe2 0x80 0x82 |
    0xe2 0x80 0x83 |
    0xe2 0x80 0x84 |
    0xe2 0x80 0x85 |
    0xe2 0x80 0x86 |
    0xe2 0x80 0x87 |
    0xe2 0x80 0x88 |
    0xe2 0x80 0x89 |
    0xe2 0x80 0x8a |
    0xe2 0x80 0xaf |
    0xe2 0x81 0x9f |
    0xe3 0x80 0x80;

WhiteSpace = TAB | VT | FF | SP | NBSP | BOM | U_Zs;

LineTerminatorSequence =
    ( LF |
      CR |
      LS |
      PS |
      CR LF ) %{ line++; pline = p; };

Space =
    WhiteSpace*
    ( LineTerminatorSequence WhiteSpace* )*;

Sign = "+" | "-" %{ negate(); };

Digit = [0-9];
Digits = Digit | [1-9] Digit*;

Number =
    # Sign
    (
        "+" | "-" %{ numberParser.negate(); }
    ) ?
    # Read whole number part
    Digits >{ mark = p; } %{
        numberParser.integer(readBytes(mark, p));
    }
    # Read fraction part
    (
        "." ( Digit* ) >{ mark = p; } %{
            numberParser.fraction(readBytes(mark, p));
        }
    )?
    # Read exponent part
    (
        [Ee]
        (
            "+" | "-" %{ numberParser.negateExponent(); }
        )?
        Digits >{ mark = p; } %{
            numberParser.exponent(readBytes(mark, p));
        }
    ) ?;

BasicCharacter =
    ^ ( cntrl | '"' | '\\' );

HexChar = [0-9a-fA-F];

CharacterEscape =
    '"'  %{ stringParser.appendChar('"');  } |
    '\\' %{ stringParser.appendChar('\\'); } |
    '/'  %{ stringParser.appendChar('/');  } |
    'b'  %{ stringParser.appendChar('\b'); } |
    'f'  %{ stringParser.appendChar('\f'); } |
    'n'  %{ stringParser.appendChar('\n'); } |
    'r'  %{ stringParser.appendChar('\r'); } |
    't'  %{ stringParser.appendChar('\t'); } |
    "u" ( HexChar HexChar HexChar HexChar ) >{ mark = p; } %{
        stringParser.appendUnicodeChar(readBytes(mark, p));
    };

BasicCharacters =
    ( BasicCharacter* ) >{ mark = p; } %{
        stringParser.append(readByteBuffer(mark, p));
    };

Characters =
    BasicCharacters ( '\\' CharacterEscape BasicCharacters )*;

String =
    '"' Characters '"';

main := |*
    Space;
    Number >{ numberParser.reset(); } => {
        event = SerdeEvent.VALUE_NUMBER;
        fbreak;
    };
    String >{ stringParser.reset(); } => {
        event = SerdeEvent.VALUE_STRING;
        fbreak;
    };
    "{" => {
        event = SerdeEvent.OBJECT_START;
        fbreak;
    };
    "}" => {
        event = SerdeEvent.OBJECT_END;
        fbreak;
    };
    "[" => {
        event = SerdeEvent.ARRAY_START;
        fbreak;
    };
    "]" => {
        event = SerdeEvent.ARRAY_END;
        fbreak;
    };
    "null" => {
        event = SerdeEvent.VALUE_NULL;
        fbreak;
    };
    "true" => {
        booleanValue = true;
        event = SerdeEvent.VALUE_BOOLEAN;
        fbreak;
    };
    "false" => {
        booleanValue = false;
        event = SerdeEvent.VALUE_BOOLEAN;
        fbreak;
    };
    "," => {
        if (!handleComma()) {
            event = SerdeEvent.ERROR;
            fbreak;
        }
    };
    ":" => {
        if (!handleColon()) {
            event = SerdeEvent.ERROR;
            fbreak;
        }
    };
    any => {
        handleUnexpectedChar();
        event = SerdeEvent.ERROR;
        fbreak;
    };
*|;

}%%