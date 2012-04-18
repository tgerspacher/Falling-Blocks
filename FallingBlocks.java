import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

// author: Todd Gerspacher
// use jpanel so it can be placed in either jframe or japplet
public class FallingBlocks extends JPanel implements KeyListener, ActionListener
{
    // dimensions of board
    public static final int NUMBER_COLUMNS = 10;
    public static final int NUMBER_ROWS = 15;

    private Color board[][] = new Color[NUMBER_ROWS][NUMBER_COLUMNS];

    // how many pixels wide and high to draw a Cell
    public static final int CELL_SIZE = 20;

    public static final int BOARD_WIDTH = CELL_SIZE * NUMBER_COLUMNS;
    public static final int BOARD_HEIGHT = CELL_SIZE * NUMBER_ROWS;
    public static final int NEXT_BOARD = CELL_SIZE * 6;

    // all the shape's starting center position
    private static final int CENTER_ROWS[] = {2, 1, 1, 0, 0, 1, 1};
    private static final int CENTER_COLUMNS[] = {4, 4, 4, 4, 5, 4, 5};

    // all the shape's starting position in board (relative to it's center)
    private static final int STARTING_ROWS[][] = {{-2, -1, 0, 1}, {-1, 0, 0, 0}, {-1, -1, 0, 0},
        {0, 0, 1, 2}, {0, 0, 1, 2}, {-1, -1, 0, 0}, {-1, -1, 0, 0}};

    private static final int STARTING_COLUMNS[][] = {{0, 0, 0, 0}, {0, -1, 0, 1}, {0, 1, 0, 1},
        {0, 1, 0, 0}, {-1, 0, 0, 0}, {0, 1, -1, 0}, {-1, 0, 0, 1}};

    // variables for current shape
    private int shapeRows[] = new int[4];
    private int shapeColumns[] = new int[4];
    private Color shapeColor;
    private int centerRow, centerColumn;

    // variables for next shape
    private int nextShape;
    private Color nextColor;

    // the preferred width and height for displaying the jpanel
    private Dimension preferredSize;

    // timer to automatically drop shape
    private Timer dropClock = new Timer(200, this);

    // timer to pause game when rows are highlighted
    private Timer highlightClock = new Timer(100, this);

    // The following are for double buffered graphics.
    // The idea is to draw everything on an offscreen graphic, sort of like a flip-book.
    // Do this so that everytime the paint method is called, it has little to redraw
    private Image image;
    private Graphics graphics;

    // position to draw the score
    private int scoreX, scoreY;

    private Random random = new Random();
    private int score = 0;

    public FallingBlocks()
    {
        addKeyListener(this);

        clearBoard();
        nextShape();
        createShape();

       // The height of the JPanel needs to be the height of the board.
       // Plus space above and below it.
       int height = BOARD_HEIGHT + (CELL_SIZE * 2);

        // The width of the JPanel needs to be the width of
        //  both the board and the window that shows the next shape.
        /// Also need to include the spaces before, after, and between these windows.
        int width = BOARD_WIDTH + NEXT_BOARD + (CELL_SIZE * 3);

        preferredSize = new Dimension(width, height);

        setPreferredSize( preferredSize );
    }

    private void clearBoard()
    {
        for (int row = 0; row < NUMBER_ROWS; row++)
        {
            for (int column = 0; column < NUMBER_COLUMNS; column++)
            {
                board[row][column] = Color.BLACK;
            }
        }
    }

    private void nextShape()
    {
        // SHAPE I = 0
        // SHAPE T = 1
        // SHAPE O = 2
        // SHAPE L = 3
        // SHAPE J = 4
        // SHAPE S = 5
        // SHAPE Z = 6

        // set up next shape to create
        nextShape = random.nextInt(7);

        // NOTE: Add 1 to prevent the Color black from being chosen
        int red = random.nextInt(255) + 1;
        int green = random.nextInt(255) + 1;
        int blue = random.nextInt(255) + 1;

        nextColor = new Color(red, green, blue);
    }

    private void createShape()
    {
        // create shape
        centerRow = CENTER_ROWS[nextShape];
        centerColumn = CENTER_COLUMNS[nextShape];

        System.arraycopy(STARTING_ROWS[nextShape], 0, shapeRows, 0, 4);
        System.arraycopy(STARTING_COLUMNS[nextShape], 0, shapeColumns, 0, 4);

        shapeColor = nextColor;

        nextShape();
    }

    public void paint(Graphics g)
    {
        if( image == null )
        {
            image = createImage(preferredSize.width, preferredSize.height);
        }

        if( graphics == null )
        {
            graphics = image.getGraphics();

            // NOTE: Using a black background for tower solves problem when shape moves
            //          and their's a residue left behind.
            graphics.setColor(Color.BLACK);

            // NOTE: Add an extra one pixel to width and height so it doesn't look like
            //          shape moves sightly out of bounds
            graphics.fillRect(CELL_SIZE, CELL_SIZE, BOARD_WIDTH + 1, BOARD_HEIGHT + 1);

            String str = "Score: ";
            // determine font and line spacing dimensions
            FontMetrics fontMetric = graphics.getFontMetrics();
            int lineSpace = fontMetric.getAscent() + fontMetric.getDescent();

            int x = (CELL_SIZE * 2) + BOARD_WIDTH;
            int y = scoreY = (CELL_SIZE * 2) + NEXT_BOARD;

            scoreX = fontMetric.stringWidth(str) + x;

            graphics.drawString(str, x, y);
            y += lineSpace;
            graphics.drawString("~ Instructions ~", x, y);
            y += lineSpace;
            graphics.drawString("rotate ~ up arrow", x, y);
            y += lineSpace;
            graphics.drawString("move left ~ left arrow", x, y);
            y += lineSpace;
            graphics.drawString("move right ~ left arrow", x, y);
            y += lineSpace;
            graphics.drawString("move down ~ down arrow or space bar", x, y);
            y += lineSpace;
            graphics.drawString("pause/play ~ p key", x, y);

            drawNextShape();
        }

        drawShape( shapeColor );

        // place buffered graphics onto panel
        g.drawImage(image, 0, 0, this);

        // draw score outside of everything so don't have to worry about erasing old score
        g.drawString("" + score, scoreX, scoreY);
    }

    private void drawNextShape()
    {
        int leftEdge = (CELL_SIZE * 2) + BOARD_WIDTH;
        int indx;

        // draw filled in next shape window
        // NOTE: This will get rid of previous "next shape"
        graphics.fillRect(leftEdge, CELL_SIZE, NEXT_BOARD, NEXT_BOARD);

        // draw next shape
        int rows[] = new int[4];
        int columns[] = new int[4];

        System.arraycopy(STARTING_ROWS[nextShape], 0, rows, 0, 4);
        System.arraycopy(STARTING_COLUMNS[nextShape], 0, columns, 0, 4);

        int x, y;

        for(indx = 0; indx < 4; indx++)
        {
            x = leftEdge + ((columns[indx] + 2) * CELL_SIZE);
            y = CELL_SIZE + ((rows[indx] + 2) * CELL_SIZE);

            // color the inside of the square
            graphics.setColor( nextColor);
            graphics.fillRect(x, y, CELL_SIZE, CELL_SIZE);

            // draw the outline of the square
            graphics.setColor(Color.BLACK);
            graphics.drawRect(x, y, CELL_SIZE, CELL_SIZE);
        }
    }

    // allow passing in of color to allow shape to either be drawn or erased
    private void drawShape(Color color)
    {
        int x, y;

        for(int indx = 0; indx < 4; indx++)
        {
            x = CELL_SIZE + ((shapeColumns[indx] + centerColumn) * CELL_SIZE);
            y = CELL_SIZE + ((shapeRows[indx] + centerRow) * CELL_SIZE);

            // color the inside of the square
            graphics.setColor(color);
            graphics.fillRect(x, y, CELL_SIZE, CELL_SIZE);

            // draw the outline of the square
            graphics.setColor(Color.BLACK);
            graphics.drawRect(x, y, CELL_SIZE, CELL_SIZE);
        }
    }

    public void keyReleased(KeyEvent evt){}
    public void keyTyped(KeyEvent evt){}
    
    public void keyPressed(KeyEvent e)
    {
        int keyCode = e.getKeyCode();

        if(keyCode == KeyEvent.VK_LEFT)
        {
            if( dropClock.isRunning() )
            {
                left();
            }
        }
        else if(keyCode == KeyEvent.VK_RIGHT)
        {
            if( dropClock.isRunning() )
            {
                right();
            }
        }
        else if((keyCode == KeyEvent.VK_DOWN) || (keyCode == KeyEvent.VK_SPACE))
        {
            if( dropClock.isRunning() )
            {
                drop();
            }
        }
        else if(keyCode == KeyEvent.VK_UP)
        {
            if( dropClock.isRunning() )
            {
                rotate();
            }
        }
        else if(keyCode == KeyEvent.VK_P)
        {
            if( dropClock.isRunning() )
            {
                dropClock.stop();
            }
            else if( !highlightClock.isRunning() )
            {
                dropClock.start();
            }
        }
    }   

    private void left()
    {
        int column;

        // check that all shapes can be moved left/right
        for(int indx = 0; indx < 4; indx++)
        {
            // coordinates of shapes blocks are relative to center
            column = shapeColumns[indx] + centerColumn - 1;

            // check if cell is out of bounds or overlapping
            // NOTE: uses short circuit evaluation to avoid ArrayIndexOutOfBoundsException
            if((column < 0) || (board[shapeRows[indx] + centerRow][column] != Color.BLACK))
            {
                return;
            }
        }

        // erase old Shape
        drawShape( Color.BLACK );

        // move shape's center
        centerColumn = centerColumn - 1;

        // draw Shape in new position
        repaint();
    }

    private void right()
    {
        int column;

        for(int indx = 0; indx < 4; indx++)
        {
            // coordinates of shapes blocks are relative to center
            column = shapeColumns[indx] + centerColumn + 1;

            // check if cell is out of bounds or overlapping
            // NOTE: uses short circuit evaluation to avoid ArrayIndexOutOfBoundsException
            if((column >= NUMBER_COLUMNS) || (board[shapeRows[indx] + centerRow][column] != Color.BLACK))
            {
                return;
            }
        }

        // erase old Shape
        drawShape( Color.BLACK );

        // move shape's center
        centerColumn = centerColumn + 1;

        // draw Shape in new position
        repaint();
    }

    private void drop()
    {
        int row;

        // check that all shapes can be moved down
        for(int indx = 0; indx < 4; indx++)
        {
            // coordinates of shapes blocks are relative to center
            row = shapeRows[indx] + centerRow + 1;

            // NOTE: uses short circuit evaluation to avoid ArrayIndexOutOfBoundsException

            // at bottom of tower?
            if((row >= NUMBER_ROWS) || (board[row][shapeColumns[indx] + centerColumn] != Color.BLACK))
            {
                // stop automatically dropping the shape
                dropClock.stop();

                // place shape in board
                for(indx = 0; indx < 4; indx++)
                {
                    board[shapeRows[indx] + centerRow][shapeColumns[indx] + centerColumn] = shapeColor;
                }

                // highlight any filled rows
                int x, y, column;

                for(indx = 0; indx < 4; indx++)
                {
                    row = shapeRows[indx] + centerRow;

                    if( isFilledRow(row) )
                    {
                        y = CELL_SIZE + (row * CELL_SIZE);

                        for(column = 0; column < NUMBER_COLUMNS; column++)
                        {
                            x = CELL_SIZE + (column * CELL_SIZE);

                            // color the inside of the square
                            graphics.setColor( Color.WHITE );
                            graphics.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                            // draw the outline of the square
                            graphics.setColor( Color.BLACK );
                            graphics.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                        }

                        score++;
                    }
                }

                repaint();

                // indicate that rows may need to be dropped
                highlightClock.start();
                return;
            }
        }

        // erase old Shape
        drawShape( Color.BLACK );

        // only need to move shape's center
        centerRow = centerRow + 1;

        // draw Shape in new position
        repaint();
    }

    private boolean isFilledRow(int row)
    {
        for(int column = 0; column < NUMBER_COLUMNS; column++)
        {
            // if there's an empty square in row
            if(board[row][column] == Color.BLACK)
            {
                return false;
            }
        }

        return true;
    }

    public void rotate()
    {
	int i, temp, row, column;

	int tempRows[] = new int[4];
        int tempColumns[] = new int[4];

	// roate temporary shape
	for(i = 0; i < 4; i++)
	{
            temp = shapeColumns[i];
            tempColumns[i] = -shapeRows[i];
            tempRows[i] = temp;
	}

	// check that temporary shape is good
	for(i = 0; i < 4; i++)
	{
            // get real location of squares
            row = tempRows[i] + centerRow;
            column = tempColumns[i] + centerColumn;

            // NOTE: uses short circuit evaluation to avoid ArrayIndexOutOfBoundsException

            // check squares are within width of board
            if((column < 0) || (column >= NUMBER_COLUMNS) ||
		// check square are within height of board
                (row < 0) || (row >= NUMBER_ROWS) ||
		// check squares are not overlapping a square on board
		(board[row][column] != Color.BLACK))
            {
                return;
            }
	}

        // erase old shape
        drawShape( Color.BLACK );

        // copy temporary shape to current shape
	System.arraycopy(tempRows, 0, shapeRows, 0, 4);
        System.arraycopy(tempColumns, 0, shapeColumns, 0, 4);

        repaint();
    }

    public void actionPerformed(ActionEvent evt)
    {
        // NOTE: since drop shape is more likely, it's tested first
        if(evt.getSource() == dropClock)
        {
            drop();
        }
        else
        {
            highlightClock.stop();

            int indx, row, column;
            int x, y;

            // move rows down to fill in filled rows
            for(indx = 0; indx < 4; indx++)
            {
                row = shapeRows[indx] + centerRow;

                if( isFilledRow(row) )
                {
                    // move rows down by copying the row that is above it
                    for(column = row; column > 0; column--)
                    {
                        System.arraycopy(board[column - 1], 0, board[column], 0, NUMBER_COLUMNS);
                    }
                }
            }

            // re-draw board
            // NOTE: It's okay if this takes awhile, because user will expect it to.
            // NOTE: This will also erase highlighted rows
            for(row = 0; row < NUMBER_ROWS; row++)
            {
                for(column = 0; column < NUMBER_COLUMNS; column++)
                {
                    x = CELL_SIZE + (column * CELL_SIZE);
                    y = CELL_SIZE + (row * CELL_SIZE);

                    // color the inside of the square
                    graphics.setColor( board[row][column] );
                    graphics.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                    // draw the outline of the square
                    graphics.setColor( Color.BLACK );
                    graphics.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }

            createShape();
            drawNextShape();
            repaint();

            // NOTE: Allow user to see updated screen before dropping rows again
            dropClock.start();
        }
    }

    // test FallingBlocks
    public static void main(String[] args)
    {
        FallingBlocks fallingBlocks = new FallingBlocks();

        JFrame frame = new JFrame("~ Falling Blocks ~");
        frame.setContentPane( fallingBlocks );

        frame.addKeyListener( fallingBlocks );

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
