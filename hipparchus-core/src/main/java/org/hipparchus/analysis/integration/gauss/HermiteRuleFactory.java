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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.hipparchus.util.Precision;

/**
 * Factory that creates a
 * <a href="http://en.wikipedia.org/wiki/Gauss-Hermite_quadrature">
 * Gauss-type quadrature rule using Hermite polynomials</a>
 * of the first kind.
 * Such a quadrature rule allows the calculation of improper integrals
 * of a function
 * <p>
 *  \(f(x) e^{-x^2}\)
 * </p><p>
 * Recurrence relation and weights computation follow
 * <a href="http://en.wikipedia.org/wiki/Abramowitz_and_Stegun">
 * Abramowitz and Stegun, 1964</a>.
 * </p><p>
 * The coefficients of the standard Hermite polynomials grow very rapidly.
 * In order to avoid overflows, each Hermite polynomial is normalized with
 * respect to the underlying scalar product.
 * The initial interval for the application of the bisection method is
 * based on the roots of the previous Hermite polynomial (interlacing).
 * Upper and lower bounds of these roots are provided by </p>
 * <blockquote>
 *  I. Krasikov,
 *  <em>Nonnegative quadratic forms and bounds on orthogonal polynomials</em>,
 *  Journal of Approximation theory <b>111</b>, 31-49
 * </blockquote>
 *
 */
public class HermiteRuleFactory extends AbstractRuleFactory {
    /** &pi;<sup>1/2</sup> */
    private static final double SQRT_PI = 1.77245385090551602729;
    /** &pi;<sup>-1/4</sup> */
    private static final double H0 = 7.5112554446494248286e-1;
    /** &pi;<sup>-1/4</sup> &radic;2 */
    private static final double H1 = 1.0622519320271969145;

    /** {@inheritDoc} */
    @Override
    protected Pair<double[], double[]> computeRule(int numberOfPoints)
        throws MathIllegalArgumentException {

        final double[] points = new double[numberOfPoints];
        final double[] weights = new double[numberOfPoints];

        if (numberOfPoints == 1) {
            // Break recursion.
            points[0]  = 0;
            weights[0] = SQRT_PI;
            return new Pair<>(points, weights);
        }

        // Get previous rule.
        // If it has not been computed yet it will trigger a recursive call
        // to this method.
        final int lastNumPoints = numberOfPoints - 1;
        final double[] previousPoints = getRule(lastNumPoints).getFirst();
        final NormalizedHermite hm = new NormalizedHermite(numberOfPoints - 1);
        final NormalizedHermite h  = new NormalizedHermite(numberOfPoints);
        final double tol = 10 * Precision.EPSILON;
        final BracketedUnivariateSolver<UnivariateFunction> solver = new BracketingNthOrderBrentSolver(tol, tol, tol, 5);

        final double sqrtTwoTimesLastNumPoints = FastMath.sqrt(2 * lastNumPoints);
        final double sqrtTwoTimesNumPoints = FastMath.sqrt(2 * numberOfPoints);

        // Find i-th root of H[n+1]
        final int iMax = numberOfPoints / 2;
        for (int i = 0; i < iMax; i++) {
            // Lower-bound of the interval.
            double a = (i == 0) ? -sqrtTwoTimesLastNumPoints : previousPoints[i - 1];
            // Upper-bound of the interval.
            double b = (iMax == 1) ? -0.5 : previousPoints[i];
            // find root
            final double c = solver.solve(1000, h, a, b, AllowedSolution.ANY_SIDE);
            points[i] = c;

            final double d = sqrtTwoTimesNumPoints * hm.value(c);
            final double w = 2 / (d * d);

            points[i] = c;
            weights[i] = w;

            final int idx = lastNumPoints - i;
            points[idx] = -c;
            weights[idx] = w;
        }

        // If "numberOfPoints" is odd, 0 is a root.
        // Note: as written, the test for oddness will work for negative
        // integers too (although it is not necessary here), preventing
        // a FindBugs warning.
        if (numberOfPoints % 2 != 0) {
            double hmz = H0;
            for (int j = 1; j < numberOfPoints; j += 2) {
                final double jp1 = j + 1;
                hmz = -FastMath.sqrt(j / jp1) * hmz;
            }
            final double d = sqrtTwoTimesNumPoints * hmz;
            points[iMax] = 0d;
            weights[iMax]= 2 / (d * d);

        }

        return new Pair<>(points, weights);

    }

    /** Hermite polynomial, normalized to avoid overflow. */
    private class NormalizedHermite implements UnivariateFunction {

        /** Degree. */
        private int degree;

        /** Simple constructor.
         * @param degree polynomial degree
         */
        NormalizedHermite(int degree) {
            this.degree = degree;
        }

        /** {@inheritDoc} */
        @Override
        public double value(double x) {
            double hm = H0;
            double h  = H1 * x;
            for (int j = 1; j < degree; j++) {
                // Compute H[j+1](c)
                final double jp1 = j + 1;
                final double hp = x * h * FastMath.sqrt(2 / jp1) -
                                  hm * FastMath.sqrt(j / jp1);
                hm = h;
                h  = hp;
            }
            return h;
        }

    }

}
