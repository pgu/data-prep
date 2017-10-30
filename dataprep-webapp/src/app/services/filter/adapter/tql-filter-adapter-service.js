/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import { Parser } from '@talend/daikon-tql-client';

export const CONTAINS = 'contains';
export const EXACT = 'exact';
export const INVALID_RECORDS = 'invalid_records';
export const VALID_RECORDS = 'valid_records';
export const EMPTY_RECORDS = 'empty_records';
export const INSIDE_RANGE = 'inside_range';
export const MATCHES = 'matches';
export const QUALITY = 'quality';

export default function TqlFilterAdapterService($translate) {
	const INVALID_EMPTY_RECORDS_VALUES = [
		{
			label: $translate.instant('INVALID_EMPTY_RECORDS_LABEL'),
		},
	];

	const INVALID_RECORDS_VALUES = [
		{
			label: $translate.instant('INVALID_RECORDS_LABEL'),
		},
	];

	const VALID_RECORDS_VALUES = [
		{
			label: $translate.instant('VALID_RECORDS_LABEL'),
		},
	];

	const EMPTY_RECORDS_VALUES = [
		{
			label: $translate.instant('EMPTY_RECORDS_LABEL'),
			isEmpty: true,
			value: '',
		},
	];

	return {
		EMPTY_RECORDS_VALUES,

		createFilter,
		toTQL,
		fromTQL,
	};

	//--------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------CREATION-------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	function createFilter(
		type,
		colId,
		colName,
		editable,
		args,
		removeFilterFn
	) {
		const filter = {
			type,
			colId,
			colName,
			editable,
			args,
			removeFilterFn,
		};

		filter.__defineGetter__('badgeClass', getBadgeClass.bind(filter)); // eslint-disable-line no-underscore-dangle
		filter.__defineGetter__('value', getFilterValueGetter.bind(filter)); // eslint-disable-line no-underscore-dangle
		filter.__defineSetter__('value', value =>
			getFilterValueSetter.call(filter, value)
		); // eslint-disable-line no-underscore-dangle
		return filter;
	}

	/**
	 * @ngdoc method
	 * @name getFilterValueGetter
	 * @methodOf data-prep.services.filter.service:FilterAdapterService
	 * @description Return the filter value depending on its type. This function should be used with filter definition object binding
	 * @returns {Object} The filter value
	 */
	function getFilterValueGetter() {
		switch (this.type) {
		case CONTAINS:
		case EXACT:
			return this.args.phrase;
		case INSIDE_RANGE:
			return this.args.intervals;
		case MATCHES:
			return this.args.patterns;
		case INVALID_RECORDS:
			return INVALID_RECORDS_VALUES;
		case VALID_RECORDS:
			return VALID_RECORDS_VALUES;
		case EMPTY_RECORDS:
			return EMPTY_RECORDS_VALUES;
		case QUALITY: // TODO: refacto QUALITY filter
			if (this.args.invalid && this.args.empty) {
				return INVALID_EMPTY_RECORDS_VALUES;
			}
		}
	}

	/**
	 * @ngdoc method
	 * @name getBadgeClass
	 * @methodOf data-prep.services.filter.service:FilterAdapterService
	 * @description Return a usable class name for the filter
	 * @returns {Object} The class name
	 */
	function getBadgeClass() {
		if (this.type === QUALITY) {
			const classes = {
				[VALID_RECORDS]: !!this.args.valid,
				[EMPTY_RECORDS]: !!this.args.empty,
				[INVALID_RECORDS]: !!this.args.invalid,
			};

			return Object.keys(classes)
				.filter(n => classes[n])
				.join(' ');
		}

		return this.type;
	}

	/**
	 * @ngdoc method
	 * @name getFilterValueSetter
	 * @methodOf data-prep.services.filter.service:FilterAdapterService
	 * @description Set the filter value depending on its type. This function should be used with filter definition object binding
	 * @returns {Object} The filter value
	 */
	function getFilterValueSetter(newValue) {
		switch (this.type) {
		case CONTAINS:
		case EXACT:
			this.args.phrase = newValue;
			break;
		case INSIDE_RANGE:
			this.args.intervals = newValue;
			break;
		case MATCHES:
			this.args.patterns = newValue;
		}
	}

	//--------------------------------------------------------------------------------------------------------------
	// ---------------------------------------------------CONVERTION-------------------------------------------------
	// -------------------------------------------------FILTER ==> TQL----------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	function toTQL(filters) {
		return Parser.parse(filters).serialize();
	}

	//--------------------------------------------------------------------------------------------------------------
	// ---------------------------------------------------CONVERTION-------------------------------------------------
	// -------------------------------------------------TQL ==> FILTER----------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	function fromTQL(tql) {}
}
