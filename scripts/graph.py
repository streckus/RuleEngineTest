#!/usr/bin/python

"""A library of graph/configuration functions"""

import os
import sys
import operator

class GraphError (ValueError): pass

#================================== Graph ====================================
class Graph:
   """A graph is defined by its attributes nodecount and edges.
      Its nodes are 0..nodecount-1 and edges is a list of pairs of integers
   """

   def __init__(self, name, nodecount, edges):
      self.name = name
      self.nodecount = nodecount
      self.edges = edges


   def __eq__(self, g):
      """Are self and g equal? Two graphs are equal iff the identity function
      on [0..nodecount] is an isomorphism"""
      if self.nodecount != g.nodecount  or  len(self.edges) != len(g.edges):
	 return 0
      for e in self.edges:
	 if not (e[0],e[1]) in g.edges  and  not (e[1],e[0]) in g.edges:
	    return 0
      return 1

   def __ne__(self, g):
      return not self == g

   def isIsomorphic(self, g):
      """Are self and g isomorphic?"""
      if self.nodecount != g.nodecount  or  len(self.edges) != len(g.edges):
	 return 0
      limit = countselections(self.nodecount, g.nodecount)
      if limit <= sys.maxint:
	 for i in xrange(0, countselections(self.nodecount, g.nodecount)):
	    sel = getselection(self.nodecount, g.nodecount, i)
	    if g == self.subgraph("", sel):
	       return 1
	 return 0
      else:
	 return self.issubgraphVF(g)  and  g.issubgraphVF(self)


   def subgraph(self, name, nodes):
      """Return the subgraph defined by the given list of nodes, with name
      name. The number of a node in the resulting subgraph is defined by its
      location in nodes.  For example, [2,0,1] will renumber vertices as 2->0,
      0->1, 1->2."""
      newedges = list()
      for (u,v) in self.edges:
	 if u in nodes  and  v in nodes:
	    newedges.append((nodes.index(u), nodes.index(v)))
      return Graph(name, len(nodes), newedges)

   def issubgraph(self, f):
      """Return true if self is an induced subgraph of f"""
      for i in xrange(0, countselections(f.nodecount, self.nodecount)):
	 sel = getselection(f.nodecount, self.nodecount, i)
	 if self == f.subgraph("", sel):
	    # print sel
	    return 1
      return 0

   def issubgraphVF(self, f):
      """Return true if self is an induced subgraph of f"""
      fi, fo = os.popen2("vf", 't')
      # print small
      print >>fi, self.nodecount
      for e in self.edges:
	 print >>fi, e[0], e[1]
      print >>fi, -1
      # print large
      print >>fi, f.nodecount
      for e in f.edges:
	 print >>fi, e[0], e[1]
      print >>fi, -1
      fi.close();
      # test result
      res = int(fo.readline())
      fo.close();
      if res != 0  and  res != 1:
	 print >>sys.stderr, "VFLib error"
	 return 0;
      return res;

   def complement(self):
      """Return the complement graph of self"""
      coedges = list()
      for i in xrange(self.nodecount-1):
	 for j in xrange(i+1, self.nodecount):
	    if (i,j) not in self.edges  and  (j,i) not in self.edges:
	       coedges.append((i,j))
      return Graph('co-'+self.name, self.nodecount, coedges)


   def toXML(self):
      """Return a string defining this graph as ISGCI XML"""
      res = '<simple name="%s">\n' \
	    '   <nodes count="%d"/>\n' \
	    '   <edges>\n' % (self.name, self.nodecount)
      for e in self.edges:
	 res += '      %d - %d;\n' % (e[0], e[1])
      res += '   </edges>\n' \
	     '</simple>'
      return res

   def toGraph6(self):
      """Return a graph6 string representation of this graph"""
      if self.nodecount > 62:
	 raise GraphError, "Only up to 62 nodes supported for graph6"
      res = chr(63 + self.nodecount)
      bit = 5	# Count down 5..0
      cur = 0	# Gathers bits
      for i in xrange(1, self.nodecount):
	 for j in xrange(0, i):
	    if (i,j) in self.edges  or  (j,i) in self.edges:
	       cur += 1 << bit
	    bit -= 1
	    if bit < 0:
	       res += chr(63 + cur)
	       cur = 0
	       bit = 5
	    #print i, j, bit
      if bit < 5:
	 res += chr(63 + cur)
      return res



#============================ Configuration =================================

class ConfigIter:
   """Iterator over the graphs in a configuration"""
   def __init__(self, config):
      self.config = config
      self.n = len(config)
      self.i = 0

   def __iter__(self):
      return self

   def next(self):
      if self.i >= self.n:
	 raise StopIteration
      res = self.config[self.i]
      self.i += 1
      return res


class Configuration:
   """A configuration is defined by its attributes nodecount, edges, mayedges,
      nonedges.  Its nodes are 0..nodecount-1 and edges, mayedges, nonedges is
      a list of pairs of integers. mayedges are the edges that may or may not
      be present.
   """

   def __init__(self, name, nodecount, edges, mayedges, nonedges):
      self.name = name
      self.nodecount = nodecount
      self.edges = edges
      self.mayedges = mayedges
      self.nonedges = nonedges
      if nonedges:
	 #raise GraphError, "nonedges not yet tested"
	 self.mayedges = list()
	 for i in xrange(0, nodecount-1):
	    for j in xrange(i+1, nodecount):
	       if not ( (i,j) in edges  or  (j,i) in edges ) and \
	          not ( (i,j) in nonedges  or  (j,i) in nonedges ):
		  mayedges.append((i,j))

   def __eq__(self, g):
      """Are self and g equal? Two configurations are equal iff the identity
      function on [0..nodecount] is an isomorphism"""
      if self.nodecount != g.nodecount  or \
	    len(self.edges) != len(g.edges) or \
	    len(self.nonedges) != len(g.nonedges)  or \
	    len(self.mayedges) != len(g.mayedges):
	 return 0
      for e in self.edges:
	 if not (e[0],e[1]) in g.edges  and  not (e[1],e[0]) in g.edges:
	    return 0
      for e in self.mayedges:
	 if not (e[0],e[1]) in g.mayedges  and  not (e[1],e[0]) in g.mayedges:
	    return 0
      for e in self.nonedges:
	 if not (e[0],e[1]) in g.nonedges  and  not (e[1],e[0]) in g.nonedges:
	    return 0
      return 1

   def __iter__(self):
      """Return an iterator over the graphs of this configuration"""
      return ConfigIter(self)

   def __len__(self):
      """Return the number of graphs specified by self"""
      return 2**len(self.mayedges)


   def __getitem__(self, i):
      """Return the i-th graph specified by this configuration"""
      if not isinstance(i, int):
	 raise TypeError
      if i < 0:
	 i += len(self)
      if i >= len(self):
	 raise IndexError
      thisedges = self.edges[:]
      for j in xrange(0, len(self.mayedges)):
	 if (i & (1<<j)) != 0:
	    thisedges.append(self.mayedges[j])
      return Graph("%s-%d" % (self.name, i), self.nodecount, thisedges)

   def isIsomorphic(self, g):
      """Are self and g isomorphic?"""
      if self.nodecount != g.nodecount  or \
	    len(self.edges) != len(g.edges) or \
	    len(self.nonedges) != len(g.nonedges)  or \
	    len(self.mayedges) != len(g.mayedges):
	 return 0
      for i in xrange(0, countselections(self.nodecount, g.nodecount)):
	 sel = getselection(self.nodecount, g.nodecount, i)
	 if g == self.subgraph("", sel):
	    return 1
      return 0


   def subgraph(self, name, nodes):
      """Return the subconfiguration defined by the given list of nodes, with
      name name. The number of a node in the resulting subconfiguration is
      defined by its location in nodes.  For example, [2,0,1] will renumber
      vertices as 2->0, 0->1, 1->2."""
      newedges = list()
      for (u,v) in self.edges:
	 if u in nodes  and  v in nodes:
	    newedges.append((nodes.index(u), nodes.index(v)))
      newmayedges = list()
      for (u,v) in self.mayedges:
	 if u in nodes  and  v in nodes:
	    newmayedges.append((nodes.index(u), nodes.index(v)))
      newnonedges = list()
      for (u,v) in self.nonedges:
	 if u in nodes  and  v in nodes:
	    newnonedges.append((nodes.index(u), nodes.index(v)))
      return Configuration(name, len(nodes), newedges, newmayedges, newnonedges)


   def toXML(self):
      """Return a string defining this configuration as ISGCI XML"""
      res = '<configuration name="%s">\n' \
            '   <nodes count="%d"/>\n' % (self.name, self.nodecount)
      if len(self.edges) > 0:
	 res += '   <edges>\n'
	 for e in self.edges:
	    res += '      %d - %d;\n' % (e[0], e[1])
	 res += '   </edges>\n'
      if len(self.mayedges) > 0  and  len(self.nonedges) == 0:
	 res += '   <optedges>\n'
	 for e in self.mayedges:
	    res += '      %d - %d;\n' % (e[0], e[1])
	 res += '   </optedges>\n'
      if len(self.nonedges) > 0:
	 res += '   <nonedges>\n'
	 for e in self.nonedges:
	    res += '      %d - %d;\n' % (e[0], e[1])
	 res += '   </nonedges>\n'
      res += '<link address="%s"/>\n' \
	     '<complement name="co-%s"/>\n' \
	     '</configuration>\n' % (self.name, self.name)
      return res

   def toGraph6(self):
      """Return a graph6 string representation of this graph"""
      res = Graph(self.name, self.nodecount, self.edges).toGraph6()
      if len(self.mayedges) > 0  and  len(self.nonedges) == 0:
         res += ' '+ Graph('', self.nodecount, self.mayedges).toGraph6()
      if len(self.nonedges) > 0:
         res += ' '+ Graph('', self.nodecount, self.nonedges).toGraph6()
      return res


#=============================== Selections ==================================

def countselections(n, k):
   """Return the number of different selections of k elements from n"""
   return reduce(operator.mul, range(n-k+1, n+1))


def getselection(n, k, sel):
   """Return the selection (a list of length k, with number 0..n-1) represented
   by sel"""
   result = list()
   # Total number of combinations for all yet unknown elements in the selection
   combins = countselections(n, k)
   for i in xrange(0, k):
      combins //= n-i
      x, sel = divmod(sel, combins)
      result.append(x)
   for i in xrange(k-1, -1, -1):
      for j in xrange(i-1, -1, -1):
	 result[i] += result[j] <= result[i]
   return result

#=============================== Input =====================================

_whitenodes = None

def getdata(f, n):
   _data = list()
   while len(_data) < n:
      _data += f.readline().split()
   return _data


def edge_style(style, colour):
   return style == 0 and colour == 0		# Solid black
def mayedge_style(style, colour):
   return style == 1 and colour == 4		# Dashed red
def nonedge_style(style, colour):
   return style == 1 and colour == 0		# Dashed black


def findnode(nodes, xy):
   """Find node xy in dictionary nodes, if necessary approximately"""
   try:
      return nodes[xy]
   except KeyError:
      xy = xy.split()
      x = int(xy[0])
      y = int(xy[1])
      for text,num in nodes.items():
	 n = text.split()
	 nx = int(n[0])
	 ny = int(n[1])
	 if abs(x-nx)**2 + abs(y-ny)**2 < 80**2:
	    return num
      else:
	 raise KeyError, xy


def getwhitenodes():
   return _whitenodes


def readfile(f, fname=None):
   """Return the Graph/Configuration in figfile f (with name fname)
      If there are white nodes in the drawing, a pair (Graph, List nodes)"""

   if "#FIG 3.2" != f.read(len("#FIG 3.2")):
      raise GraphError, "Not a fig 3.2 file"

   nodes = dict()
   rawnodes = list()
   global _whitenodes
   _whitenodes = list()
   rawwhitenodes = list()	# For graphs with 2 colourclasses
   edges = list()
   mayedges = list()
   nonedges = list()
   name = None
   if not fname:
      fname = f.name

   for i in xrange(8):
      f.readline()

   while 1:
      line = f.readline()
      if not line.startswith("#"):
	 break
      if line.startswith("# name="):
	 name = line[len("# name="):-1]

   while 1:
      line = f.readline()
      if not line:
	 break
      data = line.split()
      object_code = int(data[0])
      if object_code == 1:		# Ellipse
	 if int(data[1]) != 3:
	    raise GraphError, "Ellipse is not a circle defined by radius"
	 rawnodes.append((int(data[13]),int(data[12]))) # y first, then x!
	 if int(data[8]) == 20:
	    rawwhitenodes.append((int(data[13]),int(data[12])))
      elif object_code == 2:		# Polyline
	 if int(data[-2]) != 0 or int(data[-3]) != 0:
	    raise GraphError, "Polyline has arrows"
	 style = int(data[2])
	 colour = int(data[4])
	 n = int(data[-1])
	 data = getdata(f, 2*n)
	 segments = list()
	 for i in xrange(0, len(data)-4+1, 2):
	    segments.append((data[i]+" "+data[i+1],
			  data[i+2]+" "+data[i+3]))
	 if edge_style(style, colour):
	    edges.extend(segments)
	 elif mayedge_style(style, colour):
	    mayedges.extend(segments)
	 elif nonedge_style(style, colour):
	    nonedges.extend(segments)
	 else:
	    raise GraphError, "Edge (polyline) of unknown style"
      elif object_code == 5:		# Arc
	 if int(data[12]) != 0 or int(data[13]) != 0:
	    raise GraphError, "Arc has arrows"
	 if edge_style(int(data[2]), int(data[4])):
	    edges.append((data[-6]+" "+data[-5],
			  data[-2]+" "+data[-1]))
	 elif mayedge_style(int(data[2]), int(data[4])):
	    mayedges.append((data[-6]+" "+data[-5],
			     data[-2]+" "+data[-1]))
	 elif nonedge_style(int(data[2]), int(data[4])):
	    nonedges.append((data[-6]+" "+data[-5],
			     data[-2]+" "+data[-1]))
	 else:
	    raise GraphError, "Edge (arc) of unknown style"
      elif object_code == 3:		# Spline
	 if int(data[-2]) != 0 or int(data[-3]) != 0:
	    raise GraphError, "Spline has arrows"
	 style = int(data[2])
	 colour = int(data[4])
	 n = int(data[-1])
	 if n > 2:
	    print >>sys.stderr, "%s: warning: spline has >2 points" % fname
	 data = getdata(f, 3*n)		# Read control points as well
	 segments = list()
	 for i in xrange(0, 2*n-4+1, 2):
	    segments.append((data[i]+" "+data[i+1],
			  data[i+2]+" "+data[i+3]))
	 if edge_style(style, colour):
	    edges.extend(segments)
	 elif mayedge_style(style, colour):
	    mayedges.extend(segments)
	 elif nonedge_style(style, colour):
	    nonedges.extend(segments)
	 else:
	    raise GraphError, "Spline of unknown style"

   rawnodes.sort()
   for y,x in rawnodes:
      nodes[str(x)+" "+str(y)] = len(nodes)
   for y,x in rawwhitenodes:
      _whitenodes.append(findnode(nodes, str(x)+" "+str(y)))
   for i in range(len(edges)):
      edges[i] = (findnode(nodes, edges[i][0]), findnode(nodes, edges[i][1]))
   # Sort mayedges by node (top down, left right)
   for i in range(len(mayedges)):
      mayedges[i] = (findnode(nodes, mayedges[i][0]),
		     findnode(nodes, mayedges[i][1]))
      if mayedges[i][1] < mayedges[i][0]:
	 mayedges[i] = (mayedges[i][1], mayedges[i][0])
   mayedges.sort()
   # Sort nonedges by node (top down, left right)
   for i in range(len(nonedges)):
      nonedges[i] = (findnode(nodes, nonedges[i][0]),
		     findnode(nodes, nonedges[i][1]))
      if nonedges[i][1] < nonedges[i][0]:
	 nonedges[i] = (nonedges[i][1], nonedges[i][0])
   nonedges.sort()

   if name == None:
      name = fname
   if len(mayedges) == 0  and  len(nonedges) == 0:
      return Graph(name, len(nodes), edges)
   else:
      return Configuration(name, len(nodes), edges, mayedges, nonedges)



def readfilename(fname):
   """Read a Graph/Configuration from the XFig file with the given name"""
   f = open(fname)
   try:
      res = readfile(f)
   except GraphError:
      print >>sys.stderr, f.name, sys.exc_info()[1]
   except:
      print >>sys.stderr, f.name
      raise
   f.close()
   return res

# EOF
