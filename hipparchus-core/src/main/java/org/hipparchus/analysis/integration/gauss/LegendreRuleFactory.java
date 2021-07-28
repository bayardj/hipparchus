/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is not the original file distributed by the Apache Software Foundation
 * It has been modified by the Hipparchus project
 */
package org.hipparchus.analysis.integration.gauss;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.Pair;
import org.hipparchus.util.Precision;

/**
 * Factory that creates Gauss-type quadrature rule using Legendre polynomials.
 * In this implementation, the lower and upper bounds of the natural interval
 * of integration are -1 and 1, respectively.
 * The Legendre polynomials are evaluated using the recurrence relation
 * presented in <a href="http://en.wikipedia.org/wiki/Abramowitz_and_Stegun">
 * Abramowitz and Stegun, 1964</a>.
 *
 */
public class LegendreRuleFactory extends AbstractRuleFactory {

    /** {@inheritDoc} */
    @Override
    protected Pair<double[], double[]> computeRule(int numberOfPoints)
        throws MathIllegalArgumentException {

        final double[] points  = new double[numberOfPoints];
        final double[] weights = new double[numberOfPoints];

        if (numberOfPoints == 1) {
            // Break recursion.
            points[0]  = 0;
            weights[0] = 2;
            return new Pair<>(points, weights);
        }

        // Get previous rule.
        // If it has not been computed yet it will trigger a recursive call
        // to this method.
        final double[] previousPoints = getRule(numberOfPoints - 1).getFirst();
        final Legendre pm = new Legendre(numberOfPoints - 1);
        final Legendre p  = new Legendre(numberOfPoints);
        final double tol = 10 * Precision.EPSILON;
        final BracketedUnivariateSolver<UnivariateFunction> solver = new BracketingNthOrderBrentSolver(tol, tol, tol, 5);

        // Find i-th root of P[n+1]
        final int iMax = numberOfPoints / 2;
        for (int i = 0; i < iMax; i++) {
            // Lower-bound of the interval.
            double a = (i == 0) ? -1 : previousPoints[i - 1];
            // Upper-bound of the interval.
            double b = previousPoints[i];
            // find root
            final double c = solver.solve(1000, p, a, b, AllowedSolution.ANY_SIDE);
            points[i] = c;

            final double d = numberOfPoints * (pm.value(c) - c * p.value(c));
            weights[i] = 2 * (1 - c * c) / (d * d);

            // symmetrical point
            final int idx = numberOfPoints - i - 1;
            points[idx]   = -c;
            weights[idx]  = weights[i];

        }

        // If "numberOfPoints" is odd, 0 is a root.
        // Note: as written, the test for oddness will work for negative
        // integers too (although it is not necessary here), preventing
        // a FindBugs warning.
        if (numberOfPoints % 2 != 0) {
            double pmz = 1;
            for (int j = 1; j < numberOfPoints; j += 2) {
                pmz = -j * pmz / (j + 1);
            }
            final double d = numberOfPoints * pmz;
            points[iMax] = 0d;
            weights[iMax] = 2 / (d * d);
        }

        return new Pair<>(points, weights);

    }

    /** Legendre polynomial. */
    private class Legendre implements UnivariateFunction {

        /** Degree. */
        private int degree;

        /** Simple constructor.
         * @param degree polynomial degree
         */
        Legendre(int degree) {
            this.degree = degree;
        }

        /** {@inheritDoc} */
        @Override
        public double value(double x) {
            double pm = 1;
            double p  = x;
            for (int k = 1; k < degree; k++) {
                // apply recurrence relation (k+1) P_{k+1}(x) = (2k+1) x P_k(x) - k P_{k-1}(x)
                final double pp = (p * (x * (2 * k + 1)) - pm * k) / (k + 1);
                pm = p;
                p  = pp;
            }
            return p;
        }

    }

}
