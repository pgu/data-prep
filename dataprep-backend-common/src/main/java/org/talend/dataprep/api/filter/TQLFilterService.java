// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.filter;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.tql.model.*;
import org.talend.tql.parser.Tql;
import org.talend.tql.visitor.IASTVisitor;

/**
 * A {@link FilterService} implementation that parses TQL and builds a filter.
 */
public class TQLFilterService implements FilterService {

    private PredicateFilterProvider predicateFilterProvider;

    public TQLFilterService(PredicateFilterProvider predicateFilterProvider) {
        super();
        this.predicateFilterProvider = predicateFilterProvider;
    }

    @Override
    public Predicate<DataSetRow> build(String filterAsString, RowMetadata rowMetadata) {
        final TqlElement parsedPredicate = Tql.parse(filterAsString);
        return (Predicate<DataSetRow>) parsedPredicate.accept(new DataSetPredicateVisitor(predicateFilterProvider, rowMetadata));
    }

    private static class DataSetPredicateVisitor implements IASTVisitor {

        private PredicateFilterProvider predicateFilterProvider;

        private RowMetadata rowMetadata;

        DataSetPredicateVisitor(PredicateFilterProvider predicateFilterProvider, RowMetadata rowMetadata) {
            super();
            this.predicateFilterProvider = predicateFilterProvider;
            this.rowMetadata = rowMetadata;
        }

        @Override
        public Object visit(TqlElement tqlElement) {
            return null;
        }

        @Override
        public Object visit(ComparisonOperator comparisonOperator) {
            throw new NotImplementedException();
        }

        @Override
        public Object visit(LiteralValue literalValue) {
            return literalValue.getValue();
        }

        @Override
        public Object visit(FieldReference fieldReference) {
            return fieldReference.getPath();
        }

        @Override
        public Object visit(Expression expression) {
            throw new NotImplementedException();
        }

        @Override
        public Object visit(AndExpression andExpression) {
            Predicate<DataSetRow> predicate = null;
            for (Expression expression : andExpression.getExpressions()) {
                if (predicate != null) {
                    predicate = predicate.and((Predicate<DataSetRow>) expression.accept(this));
                } else {
                    predicate = (Predicate<DataSetRow>) expression.accept(this);
                }
            }
            return predicate;
        }

        @Override
        public Object visit(OrExpression orExpression) {
            Predicate<DataSetRow> predicate = null;
            for (Expression expression : orExpression.getExpressions()) {
                if (predicate != null) {
                    predicate = predicate.or((Predicate<DataSetRow>) expression.accept(this));
                } else {
                    predicate = (Predicate<DataSetRow>) expression.accept(this);
                }
            }
            return predicate;
        }

        @Override
        public Predicate<DataSetRow> visit(ComparisonExpression comparisonExpression) {
            final ComparisonOperator.Enum operator = comparisonExpression.getOperator().getOperator();
            final String columnName = (String) comparisonExpression.getField().accept(this);
            final String value = (String) comparisonExpression.getValueOrField().accept(this);

            switch (operator) {
            case EQ:
                return predicateFilterProvider.createEqualsPredicate(columnName, value);
            case LT:
                return predicateFilterProvider.createLowerThanPredicate(columnName, value);
            case GT:
                return predicateFilterProvider.createGreaterThanPredicate(columnName, value);
            case NEQ:
                return predicateFilterProvider.createEqualsPredicate(columnName, value).negate();
            case LET:
                return predicateFilterProvider.createLowerOrEqualsPredicate(columnName, value);
            case GET:
                return predicateFilterProvider.createGreaterOrEqualsPredicate(columnName, value);
            }
            return null;
        }

        @Override
        public Predicate<DataSetRow> visit(FieldInExpression fieldInExpression) {
            final String columnName = fieldInExpression.getFieldName();
            final List<String> collect = Stream.of(fieldInExpression.getValues()).map(LiteralValue::getValue).collect(Collectors.toList());

            return row -> collect.contains(row.get(columnName));
        }

        @Override
        public Predicate<DataSetRow> visit(FieldIsEmptyExpression fieldIsEmptyExpression) {
            final String columnName = fieldIsEmptyExpression.getFieldName();
            return predicateFilterProvider.createEmptyPredicate(columnName);
        }

        @Override
        public Predicate<DataSetRow> visit(FieldIsValidExpression fieldIsValidExpression) {
            final String columnName = fieldIsValidExpression.getFieldName();
            return predicateFilterProvider.createValidPredicate(columnName);
        }

        @Override
        public Predicate<DataSetRow> visit(FieldIsInvalidExpression fieldIsInvalidExpression) {
            final String columnName = fieldIsInvalidExpression.getFieldName();
            return predicateFilterProvider.createInvalidPredicate(columnName);
        }

        @Override
        public Predicate<DataSetRow> visit(FieldMatchesRegex fieldMatchesRegex) {
            final String columnName = fieldMatchesRegex.getFieldName();
            final String regex = fieldMatchesRegex.getRegex();
            final Pattern pattern = Pattern.compile(regex);

            return row -> pattern.matcher(row.get(columnName)).matches();
        }

        @Override
        public Object visit(FieldCompliesPattern fieldCompliesPattern) {
            final String columnName = fieldCompliesPattern.getFieldName();
            final String pattern = fieldCompliesPattern.getPattern();

            return predicateFilterProvider.createCompliesPredicate(columnName, pattern);
        }

        @Override
        public Predicate<DataSetRow> visit(FieldBetweenExpression fieldBetweenExpression) {
            final String columnName = fieldBetweenExpression.getFieldName();
            final String low = fieldBetweenExpression.getLeft().getValue();
            final String high = fieldBetweenExpression.getRight().getValue();

            return predicateFilterProvider.createRangePredicate(columnName, low, high, rowMetadata);
        }

        @Override
        public Predicate<DataSetRow> visit(NotExpression notExpression) {
            return ((Predicate<DataSetRow>) notExpression.getExpression().accept(this)).negate();
        }

        @Override
        public Predicate<DataSetRow> visit(FieldContainsExpression fieldContainsExpression) {
            final String columnName = fieldContainsExpression.getFieldName();
            final String value = fieldContainsExpression.getValue();

            return predicateFilterProvider.createContainsPredicate(columnName, value);
        }
    }
}
