# 520-Class-Project

Jake Magier, Dylan McFadden, Ethan Smith, Jairo Lopez, Pranjal Singhal, Rohan Kapoor

# Static analysis

Implementation of  a data-flow analysis in Soot.

The main goal of this class project is to implement a data-flow analysis in Soot that performs a dependency analysis. More specifically, the analysis should determine for a given condition whether any of its variables depends on global state or, if the condition is a loop condition, whether any of its variables depends on any computation in the loop body.

# To Run 

Install Eclipse Kepler

Install Java JDK 1.7 (only runs on 1.7)

Install soot Eclipse Plugin by following instructions at https://github.com/Sable/soot/wiki/Eclipse-Plugin-Installation

Set the project directory as workspace in Eclipse and import the projects into Eclipse.

Put classes to analyse in TestProject.

To run analysis, right click on the class to test and go to soot -> run soot -> check interactive mode, via shimple, output file shimple

