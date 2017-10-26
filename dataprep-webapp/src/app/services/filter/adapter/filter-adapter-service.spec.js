/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

import {
    CONTAINS,
    EXACT,
    INVALID_RECORDS,
    VALID_RECORDS,
    EMPTY_RECORDS,
    INSIDE_RANGE,
    MATCHES,
    QUALITY,
} from './filter-adapter-service';

describe('Filter Adapter Service', () => {

    const columns = [
        { id: '0000', name: 'firstname' },
        { id: '0001', name: 'lastname' },
        { id: '0002', name: 'birthdate' },
        { id: '0003', name: 'address' },
        { id: '0004', name: 'gender' },
    ];

    beforeEach(angular.mock.module('data-prep.services.filter-adapter'));

    beforeEach(angular.mock.module('pascalprecht.translate', ($translateProvider) => {
        $translateProvider.translations('en', {
            "INVALID_RECORDS_LABEL": "rows with invalid values",
            "VALID_RECORDS_LABEL": "rows with valid values",
            "INVALID_EMPTY_RECORDS_LABEL": "rows with invalid or empty values",
            "EMPTY_RECORDS_LABEL": "rows with empty values",
        });
        $translateProvider.preferredLanguage('en');
    }));

    describe('create filter', () => {
        it('should create filter', inject((FilterAdapterService) => {
            //given
            const type = CONTAINS;
            const colId = '0001';
            const colName = 'firstname';
            const editable = true;
            const args = {};
            const removeFilterFn = jasmine.createSpy('removeFilterFn');

            //when
            const filter = FilterAdapterService.createFilter(type, colId, colName, editable, args, removeFilterFn);

            //then
            expect(filter.type).toBe(type);
            expect(filter.colId).toBe(colId);
            expect(filter.colName).toBe(colName);
            expect(filter.editable).toBe(editable);
            expect(filter.args).toBe(args);
            expect(filter.removeFilterFn).toBe(removeFilterFn);
        }));

        describe('get value', () => {
            it('should return value on CONTAINS filter', inject((FilterAdapterService) => {
                //given
                const type = CONTAINS;
                const args = {
                    phrase: [
                        {
                            value: 'Jimmy',
                        },
                    ],
                };

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, args, null);

                //then
                expect(filter.value).toEqual([
                    {
                        value: 'Jimmy',
                    },
                ]);
            }));

            it('should return value on EXACT filter', inject((FilterAdapterService) => {
                //given
                const type = EXACT;
                const args = {
                    phrase: [
                        {
                            value: 'Jimmy',
                        },
                    ],
                };

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, args, null);

                //then
                expect(filter.value).toEqual([
                    {
                        value: 'Jimmy',
                    },
                ]);
            }));

            it('should return value on INVALID_RECORDS filter', inject((FilterAdapterService) => {
                //given
                const type = INVALID_RECORDS;

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, null, null);

                //then
                expect(filter.value).toEqual([
                    {
                        label: 'rows with invalid values',
                    },
                ]);
            }));

            it('should return value on QUALITY filter', inject((FilterAdapterService) => {
                //given
                const type = QUALITY;

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, { invalid: true, empty: true }, null);

                //then
                expect(filter.value).toEqual([
                    {
                        label: 'rows with invalid or empty values',
                    },
                ]);
            }));

            it('should return value on EMPTY_RECORDS filter', inject((FilterAdapterService) => {
                //given
                const type = EMPTY_RECORDS;

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, null, null);

                //then
                expect(filter.value).toEqual([
                    {
                        label: 'rows with empty values',
                        isEmpty: true,
                    },
                ]);
            }));

            it('should return value on VALID_RECORDS filter', inject((FilterAdapterService) => {
                //given
                const type = VALID_RECORDS;

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, null, null);

                //then
                expect(filter.value).toEqual([
                    {
                        label: 'rows with valid values',
                    },
                ]);
            }));

            it('should return value on INSIDE_RANGE filter', inject((FilterAdapterService) => {
                //given
                const type = 'inside_range';
                const args = {
                    intervals: [
                        {
                            label: '[1,000 .. 2,000[',
                            value: [1000, 2000],
                        },
                    ],
                    type: 'integer',
                };

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, args, null);

                //then
                expect(filter.value).toEqual([
                    {
                        label: '[1,000 .. 2,000[',
                        value: [1000, 2000],
                    },
                ]);
            }));

            it('should return value on MATCHES filter', inject((FilterAdapterService) => {
                //given
                const type = MATCHES;
                const args = {
                    patterns: [
                        {
                            value: 'Aa9',
                        },
                    ],
                };

                //when
                const filter = FilterAdapterService.createFilter(type, null, null, null, args, null);

                //then
                expect(filter.value).toEqual([
                    {
                        value: 'Aa9',
                    },
                ]);
            }));
        });
    });

    describe('adaptation from tree', () => {
        it('should return nothing when there is no filter tree', inject((FilterAdapterService) => {
            //when
            const filters = FilterAdapterService.fromTree();

            //then
            expect(filters).toBeFalsy();
        }));

        it('should create single CONTAINS filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                contains: {
                    field: '0001',
                    value: 'Jimmy',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(CONTAINS);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({
                phrase: [
                    {
                        value: 'Jimmy',
                    },
                ],
            });
        }));

        it('should create single EXACT filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                eq: {
                    field: '0001',
                    value: 'Jimmy',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(EXACT);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({
                phrase: [
                    {
                        value: 'Jimmy',
                    },
                ],
            });
        }));

        it('should create single number INSIDE_RANGE filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                range: {
                    field: '0001',
                    start: 1000,
                    end: 2000,
                    label: '[1,000 .. 2,000[',
                    type: 'integer',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe('inside_range');
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({
                intervals: [
                    {
                        label: '[1,000 .. 2,000[',
                        value: [1000, 2000],
                    },
                ],
                type: 'integer',
            });
        }));

        it('should create single date INSIDE_RANGE filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                range: {
                    field: '0001',
                    start: -631152000000, // UTC 1950-01-01
                    end: -315619200000, // UTC 1960-01-01
                    type: 'date',
                    label: '[1950, 1960[',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe('inside_range');
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({
                intervals: [
                    {
                        label: '[1950, 1960[',
                        value: [
                            //timestamps are in the client timezone
                            new Date(1950, 0, 1).getTime(),
                            new Date(1960, 0, 1).getTime(),
                        ],
                    },
                ],
                type: 'date',
            });
        }));

        it('should create single QUALITY filter from OR subtree', inject((FilterAdapterService) => {
            //given
            const tree = {
                or: [{
                    invalid: {
                    },
                }, {
                    empty: {
                    },
                }],
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(QUALITY);
            expect(singleFilter.colId).toBe(undefined);
            expect(singleFilter.colName).toBe(undefined);
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({ invalid: true, empty: true });
        }));

        it('should create single INVALID_RECORDS filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                invalid: {
                    field: '0001',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(INVALID_RECORDS);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toBeFalsy();
        }));

        it('should create single EMPTY_RECORDS filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                empty: {
                    field: '0001',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(EMPTY_RECORDS);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toBeFalsy();
        }));

        it('should create single VALID_RECORDS filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                valid: {
                    field: '0001',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(VALID_RECORDS);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toBeFalsy();
        }));

        it('should create single MATCHES filter from leaf', inject((FilterAdapterService) => {
            //given
            const tree = {
                matches: {
                    field: '0001',
                    value: 'Aa9',
                },
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(1);

            const singleFilter = filters[0];
            expect(singleFilter.type).toBe(MATCHES);
            expect(singleFilter.colId).toBe('0001');
            expect(singleFilter.colName).toBe('lastname');
            expect(singleFilter.editable).toBe(false);
            expect(singleFilter.args).toEqual({
                patterns: [
                    {
                        value: 'Aa9',
                    },
                ],
            });
        }));

        it('should create multiple filters from tree', inject((FilterAdapterService) => {
            //given
            const tree = {
                and: [
                    {
                        and: [
                            {
                                and: [
                                    {
                                        range: {
                                            field: '0001',
                                            start: 1000,
                                            end: 2000,
                                            label: '[1,000 .. 2,000[',
                                            type: 'integer',
                                        },
                                    },
                                    {
                                        contains: {
                                            field: '0002',
                                            value: 'Jimmy',
                                        },
                                    },
                                ],
                            },
                            {
                                eq: {
                                    field: '0003',
                                    value: 'Toto',
                                },
                            },
                        ],
                    },
                    {
                        matches: {
                            field: '0004',
                            value: 'Aa9',
                        },
                    },
                ],
            };

            //when
            const filters = FilterAdapterService.fromTree(tree, columns);

            //then
            expect(filters.length).toBe(4);

            const rangeFilter = filters[0];
            expect(rangeFilter.type).toBe('inside_range');
            expect(rangeFilter.colId).toBe('0001');
            expect(rangeFilter.colName).toBe('lastname');
            expect(rangeFilter.editable).toBe(false);
            expect(rangeFilter.args).toEqual({
                intervals: [
                    {
                        label: '[1,000 .. 2,000[',
                        value: [1000, 2000],
                    },
                ],
                type: 'integer',
            });

            const containsFilter = filters[1];
            expect(containsFilter.type).toBe(CONTAINS);
            expect(containsFilter.colId).toBe('0002');
            expect(containsFilter.colName).toBe('birthdate');
            expect(containsFilter.editable).toBe(false);
            expect(containsFilter.args).toEqual({
                phrase: [
                    {
                        value: 'Jimmy',
                    },
                ],
            });

            const exactFilter = filters[2];
            expect(exactFilter.type).toBe(EXACT);
            expect(exactFilter.colId).toBe('0003');
            expect(exactFilter.colName).toBe('address');
            expect(exactFilter.editable).toBe(false);
            expect(exactFilter.args).toEqual({
                phrase: [
                    {
                        value: 'Toto',
                    },
                ],
            });

            const matchesFilter = filters[3];
            expect(matchesFilter.type).toBe(MATCHES);
            expect(matchesFilter.colId).toBe('0004');
            expect(matchesFilter.colName).toBe('gender');
            expect(matchesFilter.editable).toBe(false);
            expect(matchesFilter.args).toEqual({
                patterns: [
                    {
                        value: 'Aa9',
                    },
                ],
            });
        }));
    });
});
