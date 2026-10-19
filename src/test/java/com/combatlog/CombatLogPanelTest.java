package com.combatlog;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatLogPanelTest
{
	@Test
	public void panelPaintsDarkAndHasNoTextArea() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			CombatLogConfig config = (CombatLogConfig) Proxy.newProxyInstance(
				CombatLogConfig.class.getClassLoader(),
				new Class[]{CombatLogConfig.class},
				(proxy, method, args) ->
				{
					if (method.getReturnType() == boolean.class)
					{
						return Boolean.TRUE;
					}
					if (method.getReturnType() == int.class)
					{
						return Integer.valueOf(10);
					}
					if (method.getReturnType() == String.class)
					{
						return "";
					}
					return null;
				});
			CombatLog log = new CombatLog(20);
			CombatLogPanel panel = new CombatLogPanel(log, config, new MonsterIcons(), new WeaponIcons(null));
			log.add(new HitRecord(1_000L, "Guard", TargetKind.NPC, null, 1, null, 12, HitKind.HIT, null));
			log.add(new HitRecord(2_000L, "Guard", TargetKind.NPC, null, 2, null, 8, HitKind.HIT, null));
			panel.refresh();

			assertFalse(contains(panel.getWrappedPanel(), JTextComponent.class));
			assertFalse(contains(panel.getWrappedPanel(), JTextArea.class));
			assertEquals(PluginPanel.PANEL_WIDTH, panel.getPreferredSize().width);
			assertTrue(panel.getPreferredSize().height > 0);

			JScrollPane scrollPane = scrollPane(panel);
			JViewport viewport = scrollPane.getViewport();
			assertEquals(JViewport.SIMPLE_SCROLL_MODE, viewport.getScrollMode());
			assertEquals(ColorScheme.DARK_GRAY_COLOR, viewport.getBackground());
			assertTrue(viewport.isOpaque());
			assertEquals(ColorScheme.DARK_GRAY_COLOR, panel.getBackground());

			JFrame frame = new JFrame();
			try
			{
				frame.getContentPane().add(panel.getWrappedPanel());
				frame.setSize(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH + 20, 640);
				frame.setVisible(true);
				frame.validate();
				panel.refresh();
				frame.validate();

				java.awt.Container content = frame.getContentPane();
				BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = image.createGraphics();
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
				content.paint(graphics);
				graphics.dispose();

				int background = image.getRGB(24, image.getHeight() - 24);
				assertTrue("empty sidebar area stayed white: " + Integer.toHexString(background),
					isDark(background));
				int mid = image.getRGB(24, image.getHeight() / 2);
				assertTrue("panel surface stayed white: " + Integer.toHexString(mid), isDark(mid));
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	@Test
	public void interactionDoesNotResizeSidebarOrCoverIcons() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(RuneLiteLAF.setup());
			CombatLog log = new CombatLog(50);
			CombatLogPanel panel = new CombatLogPanel(log, config(), new MonsterIcons(), new WeaponIcons(null));
			JPanel wrapped = panel.getWrappedPanel();
			Dimension locked = new Dimension(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH, 0);
			assertEquals(locked, wrapped.getPreferredSize());
			assertTrue(wrapped.isPreferredSizeSet());

			JTabbedPane sidebar = new JTabbedPane(JTabbedPane.RIGHT);
			sidebar.setOpaque(true);
			sidebar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			sidebar.putClientProperty(FlatClientProperties.STYLE,
				"tabInsets: 2,5,2,5; variableSize: true; deselectable: true; tabHeight: 26");
			ImageIcon icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
			sidebar.insertTab(null, icon, wrapped, "Combat Log", 0);
			for (int i = 0; i < 12; i++)
			{
				JPanel other = new JPanel();
				other.setPreferredSize(new Dimension(locked));
				sidebar.insertTab(null, icon, other, "Other " + i, i + 1);
			}
			sidebar.setSelectedIndex(0);

			JFrame frame = new JFrame();
			try
			{
				JPanel content = new JPanel(null);
				content.add(sidebar);
				frame.setContentPane(content);
				frame.setSize(1000, 720);
				frame.setVisible(true);
				content.setBounds(0, 0, 1000, 700);
				layoutSidebar(content, sidebar);

				int beforeWidth = sidebar.getPreferredSize().width;
				Rectangle beforeTab = sidebar.getBoundsAt(1);
				assertTrue("icon tab missing before interaction " + beforeTab, beforeTab != null && beforeTab.width > 8);
				assertTrue("icon column overlapped the panel " + beforeTab + " content " + wrapped.getBounds(),
					beforeTab.x >= wrapped.getX() + wrapped.getWidth() - 2);

				JButton clear = findButton(panel, "Clear");
				clear.dispatchEvent(new MouseEvent(clear, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 4, 4, 0, false));
				clear.dispatchEvent(new MouseEvent(clear, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 4, 4, 0, false));
				clear.doClick();
				frame.validate();
				assertSidebarStable(wrapped, sidebar, locked, beforeWidth, "hover/click");

				scrollPane(panel).getVerticalScrollBar().setValue(30);
				frame.validate();
				assertSidebarStable(wrapped, sidebar, locked, beforeWidth, "scroll");

				for (int i = 0; i < 30; i++)
				{
					log.add(new HitRecord(1_000L + i, "Guard " + i, TargetKind.NPC, null, i, null, 10 + i, HitKind.HIT, null));
					panel.refresh();
					frame.validate();
				}
				assertSidebarStable(wrapped, sidebar, locked, beforeWidth, "hits");
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	private static void layoutSidebar(JPanel content, JTabbedPane sidebar)
	{
		int innerHeight = content.getHeight();
		sidebar.setSize(Math.max(sidebar.getWidth(), 1), innerHeight);
		int sidebarWidth = sidebar.getPreferredSize().width;
		sidebar.setBounds(content.getWidth() - sidebarWidth, 0, sidebarWidth, innerHeight);
		sidebar.validate();
	}

	private static void assertSidebarStable(JPanel wrapped, JTabbedPane sidebar, Dimension locked, int beforeWidth, String interaction)
	{
		assertEquals(interaction + " changed wrapped preferred size", locked, wrapped.getPreferredSize());
		assertEquals(interaction + " changed sidebar width from " + beforeWidth, beforeWidth, sidebar.getPreferredSize().width);
		Rectangle tab = sidebar.getBoundsAt(1);
		assertTrue(interaction + " collapsed an icon tab " + tab, tab != null && tab.width > 8 && tab.height > 8);
		assertTrue(interaction + " covered icons " + tab + " content " + wrapped.getBounds(),
			tab.x >= wrapped.getX() + wrapped.getWidth() - 2);
	}

	private static JButton findButton(Component component, String text)
	{
		if (component instanceof JButton && text.equals(((JButton) component).getText()))
		{
			return (JButton) component;
		}
		if (component instanceof java.awt.Container)
		{
			java.awt.Container container = (java.awt.Container) component;
			for (int i = 0; i < container.getComponentCount(); i++)
			{
				JButton found = findButton(container.getComponent(i), text);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static CombatLogConfig config()
	{
		return (CombatLogConfig) Proxy.newProxyInstance(
			CombatLogConfig.class.getClassLoader(),
			new Class[]{CombatLogConfig.class},
			(proxy, method, args) ->
			{
				if (method.getReturnType() == boolean.class)
				{
					return Boolean.TRUE;
				}
				if (method.getReturnType() == int.class)
				{
					return Integer.valueOf(10);
				}
				if (method.getReturnType() == String.class)
				{
					return "";
				}
				return null;
			});
	}

	private static boolean isDark(int rgb)
	{
		int red = (rgb >> 16) & 255;
		int green = (rgb >> 8) & 255;
		int blue = rgb & 255;
		return red < 80 && green < 80 && blue < 80;
	}

	private static JScrollPane scrollPane(CombatLogPanel panel)
	{
		try
		{
			java.lang.reflect.Method method = PluginPanel.class.getDeclaredMethod("getScrollPane");
			method.setAccessible(true);
			return (JScrollPane) method.invoke(panel);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

	private static boolean contains(Component component, Class<?> type)
	{
		if (type.isInstance(component))
		{
			return true;
		}
		if (component instanceof java.awt.Container)
		{
			java.awt.Container container = (java.awt.Container) component;
			for (int i = 0; i < container.getComponentCount(); i++)
			{
				if (contains(container.getComponent(i), type))
				{
					return true;
				}
			}
		}
		return false;
	}
}
