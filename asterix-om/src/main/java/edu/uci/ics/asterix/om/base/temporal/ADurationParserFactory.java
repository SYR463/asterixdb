/*
 * Copyright 2009-2011 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.asterix.om.base.temporal;

import java.io.DataOutput;
import java.io.IOException;

import edu.uci.ics.asterix.om.base.AMutableDayTimeDuration;
import edu.uci.ics.asterix.om.base.AMutableDuration;
import edu.uci.ics.asterix.om.base.AMutableYearMonthDuration;
import edu.uci.ics.asterix.om.base.IAObject;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.dataflow.common.data.parsers.IValueParser;
import edu.uci.ics.hyracks.dataflow.common.data.parsers.IValueParserFactory;

public class ADurationParserFactory implements IValueParserFactory {

    public static final IValueParserFactory INSTANCE = new ADurationParserFactory();

    private static final long serialVersionUID = 1L;

    private static final String durationErrorMessage = "Wrong Input Format for a duration/year-month-duration/day-time-duration Value";
    private static final String onlyYearMonthErrorMessage = "Only year-month fields are allowed";
    private static final String onlyDayTimeErrorMessage = "Only day-time fields are allowed";

    private static final int DECIMAL_UNIT = 10;

    private ADurationParserFactory() {

    }

    @Override
    public IValueParser createValueParser() {
        final AMutableDuration aMutableDuration = new AMutableDuration(0, 0);
        return new IValueParser() {

            @Override
            public void parse(char[] buffer, int start, int length, DataOutput out) throws HyracksDataException {
                parseDuration(buffer, start, length, aMutableDuration, ADurationParseOption.All);
                try {
                    out.writeInt(aMutableDuration.getMonths());
                    out.writeLong(aMutableDuration.getMilliseconds());
                } catch (IOException ex) {
                    throw new HyracksDataException(ex);
                }
            }
        };
    }

    private enum State {
        NOTHING_READ,
        YEAR,
        MONTH,
        DAY,
        TIME,
        HOUR,
        MIN,
        MILLISEC,
        SEC;
    };

    public enum ADurationParseOption {
        YEAR_MONTH,
        DAY_TIME,
        All
    }

    interface IStringAccessor {
        char getCharAt(int index);
    }

    public static void parseDuration(final Object durationString, final int start, int length, IAObject mutableObject,
            ADurationParseOption parseOption) throws HyracksDataException {

        int offset = 0;
        int value = 0, hour = 0, minute = 0, second = 0, millisecond = 0, year = 0, month = 0, day = 0;
        State state = State.NOTHING_READ;

        IStringAccessor charAccessor;

        if (durationString instanceof char[]) {
            charAccessor = new IStringAccessor() {
                @Override
                public char getCharAt(int index) {
                    return ((char[]) durationString)[start + index];
                }
            };
        } else if (durationString instanceof byte[]) {
            charAccessor = new IStringAccessor() {

                @Override
                public char getCharAt(int index) {
                    return (char) (((byte[]) durationString)[start + index]);
                }
            };
        } else if (durationString instanceof String) {
            charAccessor = new IStringAccessor() {

                @Override
                public char getCharAt(int index) {
                    return ((String) durationString).charAt(start + index);
                }
            };
        } else {
            throw new HyracksDataException(durationErrorMessage);
        }

        short sign = 1;
        if (charAccessor.getCharAt(offset) == '-') {
            offset++;
            sign = -1;
        }

        if (charAccessor.getCharAt(offset) != 'P') {
            throw new HyracksDataException(durationErrorMessage + ": Missing leading 'P'.");
        }

        offset++;

        for (; offset < length; offset++) {
            if (charAccessor.getCharAt(offset) >= '0' && charAccessor.getCharAt(offset) <= '9') {
                // accumulate the digit fields
                value = value * DECIMAL_UNIT + charAccessor.getCharAt(offset) - '0';
            } else {
                switch (charAccessor.getCharAt(offset)) {
                    case 'Y':
                        if (state.compareTo(State.YEAR) < 0) {
                            if (parseOption == ADurationParseOption.DAY_TIME) {
                                throw new HyracksDataException(onlyDayTimeErrorMessage);
                            }
                            year = value;
                            state = State.YEAR;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong YEAR feild.");
                        }
                        break;
                    case 'M':
                        if (state.compareTo(State.TIME) < 0) {
                            if (state.compareTo(State.MONTH) < 0) {
                                if (parseOption == ADurationParseOption.DAY_TIME) {
                                    throw new HyracksDataException(onlyDayTimeErrorMessage);
                                }
                                month = value;
                                state = State.MONTH;
                            } else {
                                throw new HyracksDataException(durationErrorMessage + ": wrong MONTH field.");
                            }
                        } else if (state.compareTo(State.MIN) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            minute = value;
                            state = State.MIN;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong MIN field.");
                        }
                        break;
                    case 'D':
                        if (state.compareTo(State.DAY) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            day = value;
                            state = State.DAY;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong DAY field");
                        }
                        break;
                    case 'T':
                        if (state.compareTo(State.TIME) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            state = State.TIME;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong TIME field.");
                        }
                        break;

                    case 'H':
                        if (state.compareTo(State.HOUR) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            hour = value;
                            state = State.HOUR;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong HOUR field.");
                        }
                        break;
                    case '.':
                        if (state.compareTo(State.MILLISEC) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            int i = 1;
                            for (; offset + i < length; i++) {
                                if (charAccessor.getCharAt(offset + i) >= '0'
                                        && charAccessor.getCharAt(offset + i) <= '9') {
                                    if (i < 4) {
                                        millisecond = millisecond * DECIMAL_UNIT
                                                + (charAccessor.getCharAt(offset + i) - '0');
                                    } else {
                                        throw new HyracksDataException(durationErrorMessage
                                                + ": wrong MILLISECOND field.");
                                    }
                                } else {
                                    break;
                                }
                            }
                            offset += i;
                            state = State.MILLISEC;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong MILLISECOND field.");
                        }
                    case 'S':
                        if (state.compareTo(State.SEC) < 0) {
                            if (parseOption == ADurationParseOption.YEAR_MONTH) {
                                throw new HyracksDataException(onlyYearMonthErrorMessage);
                            }
                            second = value;
                            state = State.SEC;
                        } else {
                            throw new HyracksDataException(durationErrorMessage + ": wrong SECOND field.");
                        }
                        break;
                    default:
                        throw new HyracksDataException(durationErrorMessage + ": wrong format for duration.");

                }
                value = 0;
            }
        }

        if (state.compareTo(State.TIME) == 0) {
            throw new HyracksDataException(durationErrorMessage + ": no time fields after time separator.");
        }

        if (mutableObject instanceof AMutableDuration) {
            ((AMutableDuration) mutableObject)
                    .setValue(sign * (year * 12 + month),
                            sign
                                    * (day * 24 * 3600 * 1000L + 3600 * 1000L * hour + 60 * minute * 1000L + second
                                            * 1000L + millisecond));
        } else if (mutableObject instanceof AMutableYearMonthDuration) {
            ((AMutableYearMonthDuration) mutableObject).setMonths(sign * (year * 12 + month));
        } else if (mutableObject instanceof AMutableDayTimeDuration) {
            ((AMutableDayTimeDuration) mutableObject)
                    .setMilliseconds(sign
                            * (day * 24 * 3600 * 1000L + 3600 * 1000L * hour + 60 * minute * 1000L + second * 1000L + millisecond));
        }
    }
}
